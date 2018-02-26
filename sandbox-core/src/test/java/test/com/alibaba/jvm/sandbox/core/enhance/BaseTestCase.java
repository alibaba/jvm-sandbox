package test.com.alibaba.jvm.sandbox.core.enhance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.ExtFilter;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.enhance.Enhancer;
import com.alibaba.jvm.sandbox.core.enhance.EventEnhancer;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.util.SandboxStringUtils;
import com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByJDK;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.*;

/**
 * Created by luanjia@taobao.com on 2017/3/9.
 */
@Ignore
public class BaseTestCase {

    protected byte[] toByteArray(final Class<?> targetClass) throws IOException {
        final InputStream is = targetClass.getResourceAsStream("/" + SandboxStringUtils.toInternalClassName(targetClass.getName()).concat(".class"));
        try {
            return IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected ClassLoader newTestClassLoader() {
        return new TestClassLoader(BaseTestCase.class.getClassLoader());
    }

    private static final AtomicInteger sequencer = new AtomicInteger(1000);

    protected Class<?> watching(final Class<?> targetClass,
                                final String targetJavaMethodName,
                                final EventListener listener,
                                final Event.Type... eventType) throws IOException, InvocationTargetException, IllegalAccessException {

        final int listenerId = sequencer.getAndIncrement();
        final ClassLoader loader = newTestClassLoader();
        final byte[] srcByteCodeArray = toByteArray(targetClass);

        final ExtFilterMatcher matcher = new ExtFilterMatcher(ExtFilter.ExtFilterFactory.make(new Filter() {
            @Override
            public boolean doClassFilter(final int access,
                                         final String javaClassName,
                                         final String superClassTypeJavaClassName,
                                         final String[] interfaceTypeJavaClassNameArray,
                                         final String[] annotationTypeJavaClassNameArray) {
                return true;
            }

            @Override
            public boolean doMethodFilter(final int access,
                                          final String javaMethodName,
                                          final String[] parameterTypeJavaClassNameArray,
                                          final String[] throwsTypeJavaClassNameArray,
                                          final String[] annotationTypeJavaClassNameArray) {
                return javaMethodName.equals(targetJavaMethodName);
            }
        }));


        final Enhancer enhancer = new EventEnhancer();
        EventListenerHandlers.getSingleton().active(listenerId, listener, eventType);
        return defineClass(
                loader,
                targetClass.getName(),
                enhancer.toByteCodeArray(
                        loader,
                        srcByteCodeArray,
                        matcher.matching(new ClassStructureImplByJDK(targetClass)).getBehaviorSignCodes(),
                        listenerId,
                        eventType
                )
        );
    }

    @BeforeClass
    public static void testBeforeClass() {
        CoreConfigure.toConfigure("", "");
    }

    @Ignore
    @Test
    public void test_sum() throws IOException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final Class<?> computerClass = watching(
                Computer.class,
                "sum",
                new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        final BeforeEvent beforeEvent = (BeforeEvent) event;
                        final int[] numberArray = (int[]) beforeEvent.argumentArray[0];
                        numberArray[0] = 40;
                        numberArray[1] = 60;
                    }
                },
                Event.Type.BEFORE
        );
        final Method computer$sumMethod = unCaughtGetClassDeclaredJavaMethod(computerClass, "sum", int[].class);
        final Object computerObject = computerClass.newInstance();
        final Integer r = unCaughtInvokeMethod(computer$sumMethod, computerObject, new int[]{1, 1});
        Assert.assertEquals(100, r.intValue());

    }

}
