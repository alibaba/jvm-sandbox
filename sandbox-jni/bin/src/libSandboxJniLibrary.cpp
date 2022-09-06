#include <jni.h>
#include <jni_md.h>
#include <jvmti.h>
#include "com_alibaba_jvm_sandbox_JniAnchorPoint.h"

static jvmtiEnv *jvmti;
static jlong tagCounter = 0;

struct LimitCounter {
    jint counter;
    jint limit;

    void init(jint limit) {
        counter = 0;
        this->limit = limit;
    }

    bool tryAcquire() {
        if (limit < 0) {
            return true;
        }
        return limit > ++counter;
    }
};

static LimitCounter limitCounter = {0, 0};

// On Agent load, init evn jvmtiEnv and enable feature
// https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#onload
extern "C"
int init_agent(JavaVM *vm, void *reserved) {
    jint rc;
    /* Get JVMTI environment */
    rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_2);
    if (rc != JNI_OK) {
        fprintf(stderr, "ERROR: Sandbox JniAnchorPoint Unable to create jvmtiEnv, call vm->GetEnv failed, error=%d\n", rc);
        return -1;
    }

    // https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#jvmtiCapabilities
    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;

    // https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#AddCapabilities
    jvmtiError error = jvmti->AddCapabilities(&capabilities);
    if (error) {
        fprintf(stderr, "ERROR: Sandbox JniAnchorPoint call JVMTI->AddCapabilities failed!%u\n", error);
        return JNI_FALSE;
    }
    return JNI_OK;
}

extern "C" JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    return init_agent(vm, reserved);
}

extern "C" JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    return init_agent(vm, reserved);
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    init_agent(vm, reserved);
    return JNI_VERSION_1_6;
}

extern "C"
jlong getTag() {
    return ++tagCounter;
}

extern "C"
jvmtiIterationControl JNICALL
HeapObjectCallback(jlong class_tag, jlong size, jlong *tag_ptr, void *user_data) {
    jlong *data = static_cast<jlong *>(user_data);
    *tag_ptr = *data;

    if (limitCounter.tryAcquire()) {
        return JVMTI_ITERATION_CONTINUE;
    } else {
        return JVMTI_ITERATION_ABORT;
    }
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_alibaba_jvm_sandbox_JniAnchorPoint_getInstances0(JNIEnv *env, jclass thisClass, jclass klass, jint limit) {
   jlong tag = getTag();
   limitCounter.init(limit);
   // https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#IterateOverInstancesOfClass
   jvmtiError error = jvmti->IterateOverInstancesOfClass(klass, JVMTI_HEAP_OBJECT_EITHER, HeapObjectCallback, &tag);

   if (error) {
     printf("ERROR: Call JVMTI->IterateOverInstancesOfClass Failed!%u\n", error);
     return NULL;
   }

   jint count = 0;
   jobject *instances;

   // https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#GetObjectsWithTags
   error = jvmti->GetObjectsWithTags(1, &tag, &count, &instances, NULL);
   if (error) {
      printf("ERROR: Call JVMTI->GetObjectsWithTags Failed!%u\n", error);
      return NULL;
   }

   jobjectArray array = env->NewObjectArray(count, klass, NULL);

   for (int i = 0; i < count; i++) {
     env->SetObjectArrayElement(array, i, instances[i]);
   }

   jvmti->Deallocate(reinterpret_cast<unsigned char *>(instances));
   return array;
}