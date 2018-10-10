package com.alibaba.jvm.sandbox.core.util.matcher.structure;

import com.alibaba.jvm.sandbox.core.util.BitUtils;
import com.alibaba.jvm.sandbox.core.util.LazyGet;
import com.alibaba.jvm.sandbox.core.util.collection.GaLRUCache;
import com.alibaba.jvm.sandbox.core.util.collection.Pair;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.PrimitiveClassStructure.Primitive;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;
import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toJavaClassName;
import static com.alibaba.jvm.sandbox.core.util.matcher.structure.PrimitiveClassStructure.mappingPrimitiveByJavaClassName;
import static org.objectweb.asm.ClassReader.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * {@link Access}的ASM实现
 */
class AccessImplByAsm implements Access {

    private final int access;

    AccessImplByAsm(int access) {
        this.access = access;
    }

    /**
     * 获取Class的ACC位码
     * <p>
     * ACC位码是一个int类型的BITMAP，
     * 参考{@link Opcodes}的{@code access flags}片段
     * </p>
     *
     * @return ACC位码
     */
    private int getAccess() {
        return access;
    }

    @Override
    public boolean isPublic() {
        return BitUtils.isIn(getAccess(), ACC_PUBLIC);
    }

    @Override
    public boolean isPrivate() {
        return BitUtils.isIn(getAccess(), ACC_PRIVATE);
    }

    @Override
    public boolean isProtected() {
        return BitUtils.isIn(getAccess(), ACC_PROTECTED);
    }

    @Override
    public boolean isStatic() {
        return BitUtils.isIn(getAccess(), ACC_STATIC);
    }

    @Override
    public boolean isFinal() {
        return BitUtils.isIn(getAccess(), ACC_FINAL);
    }

    @Override
    public boolean isInterface() {
        return BitUtils.isIn(getAccess(), ACC_INTERFACE);
    }

    @Override
    public boolean isNative() {
        return BitUtils.isIn(getAccess(), ACC_NATIVE);
    }

    @Override
    public boolean isAbstract() {
        return BitUtils.isIn(getAccess(), ACC_ABSTRACT);
    }

    @Override
    public boolean isEnum() {
        return BitUtils.isIn(getAccess(), ACC_ENUM);
    }

    @Override
    public boolean isAnnotation() {
        return BitUtils.isIn(getAccess(), ACC_ANNOTATION);
    }
}

class EmptyClassStructure implements ClassStructure {

    @Override
    public String getJavaClassName() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public ClassStructure getSuperClassStructure() {
        return null;
    }

    @Override
    public List<ClassStructure> getInterfaceClassStructures() {
        return Collections.emptyList();
    }

    @Override
    public LinkedHashSet<ClassStructure> getFamilySuperClassStructures() {
        return new LinkedHashSet<ClassStructure>();
    }

    @Override
    public Set<ClassStructure> getFamilyInterfaceClassStructures() {
        return Collections.emptySet();
    }

    @Override
    public Set<ClassStructure> getFamilyTypeClassStructures() {
        return Collections.emptySet();
    }

    @Override
    public List<ClassStructure> getAnnotationTypeClassStructures() {
        return Collections.emptyList();
    }

    @Override
    public Set<ClassStructure> getFamilyAnnotationTypeClassStructures() {
        return Collections.emptySet();
    }

    @Override
    public List<BehaviorStructure> getBehaviorStructures() {
        return Collections.emptyList();
    }

    @Override
    public Access getAccess() {
        return new AccessImplByAsm(0);
    }
}

/**
 * JDK原生类型结构体
 */
class PrimitiveClassStructure extends EmptyClassStructure {

    private final Primitive primitive;

    PrimitiveClassStructure(Primitive primitive) {
        this.primitive = primitive;
    }

    public enum Primitive {
        BOOLEAN("boolean"),
        CHAR("char"),
        BYTE("byte"),
        INT("int"),
        SHORT("short"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        VOID("void");

        private final String type;

        Primitive(final String type) {
            this.type = type;
        }
    }

    @Override
    public String getJavaClassName() {
        return primitive.type;
    }

    static Primitive mappingPrimitiveByJavaClassName(final String javaClassName) {
        for (final Primitive primitive : Primitive.values()) {
            if (primitive.type.equals(javaClassName)) {
                return primitive;
            }
        }
        return null;
    }

}

class ArrayClassStructure extends EmptyClassStructure {

    private final ClassStructure elementClassStructure;

    ArrayClassStructure(ClassStructure elementClassStructure) {
        this.elementClassStructure = elementClassStructure;
    }

    @Override
    public String getJavaClassName() {
        return new StringBuilder()
                .append(elementClassStructure.getJavaClassName())
                .append("[]")
                .toString();
    }
}

/**
 * 用ASM实现的类结构
 *
 * @author luanjia@taobao.com
 */
public class ClassStructureImplByAsm extends FamilyClassStructure {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ClassReader classReader;
    private final ClassLoader loader;
    private final Access access;

    public ClassStructureImplByAsm(final InputStream classInputStream,
                                   final ClassLoader loader) throws IOException {
        this(IOUtils.toByteArray(classInputStream), loader);
    }

    public ClassStructureImplByAsm(final byte[] classByteArray,
                                   final ClassLoader loader) {
        this.classReader = new ClassReader(classByteArray);
        this.loader = loader;
        this.access = new AccessImplByAsm(this.classReader.getAccess());
    }

    private boolean isBootstrapClassLoader() {
        return null == loader;
    }

    // 获取资源数据流
    // 一般而言可以从loader直接获取，如果获取不到那么这个类也会能加载成功
    // 但如果遇到来自BootstrapClassLoader的类就必须从java.lang.Object来获取
    private InputStream getResourceAsStream(final String resourceName) {
        return isBootstrapClassLoader()
                ? Object.class.getResourceAsStream("/" + resourceName)
                : loader.getResourceAsStream(resourceName);
    }

    // 将内部类名称转换为资源名称
    private String internalClassNameToResourceName(final String internalClassName) {
        return internalClassName + ".class";
    }

    private final static GaLRUCache<Pair, ClassStructure> classStructureCache
            = new GaLRUCache<Pair, ClassStructure>(1024);

    // 构造一个类结构实例
    private ClassStructure newInstance(final String javaClassName) {

        // 空载保护
        if (null == javaClassName) {
            return null;
        }

        // 是个数组类型
        if (javaClassName.endsWith("[]")) {
            return new ArrayClassStructure(newInstance(javaClassName.substring(0, javaClassName.length() - 2)));
        }

        // 是个基本类型
        final Primitive primitive = mappingPrimitiveByJavaClassName(javaClassName);
        if (null != primitive) {
            return new PrimitiveClassStructure(primitive);
        }

        final Pair pair = new Pair(loader, javaClassName);
        if (classStructureCache.containsKey(pair)) {
            return classStructureCache.get(pair);
        } else {
            final InputStream is = getResourceAsStream(internalClassNameToResourceName(toInternalClassName(javaClassName)));
            if (null != is) {
                try {
                    final ClassStructure classStructure = new ClassStructureImplByAsm(is, loader);
                    classStructureCache.put(pair, classStructure);
                    return classStructure;
                } catch (Throwable cause) {
                    // ignore
                    logger.warn("new instance class structure by using ASM failed, will return null. class={};loader={};",
                            javaClassName, loader, cause);
                    classStructureCache.put(pair, null);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }

//        // 是个普通Java类型
//        final InputStream is = getResourceAsStream(internalClassNameToResourceName(toInternalClassName(javaClassName)));
//        if (null != is) {
//            try {
//                return new ClassStructureImplByAsm(is, loader);
//            } catch (Throwable cause) {
//                // ignore
//                logger.warn("new instance class structure by using ASM failed, will return null. class={};loader={};",
//                        javaClassName, loader, cause);
//            } finally {
//                IOUtils.closeQuietly(is);
//            }
//        }
        // 出现异常或者找不到
        return null;
    }

    // 构造一个类结构实例数组
    private List<ClassStructure> newInstances(final String[] javaClassNameArray) {
        final List<ClassStructure> classStructures = new ArrayList<ClassStructure>();
        if (null == javaClassNameArray) {
            return classStructures;
        }
        for (final String javaClassName : javaClassNameArray) {
            final ClassStructure classStructure = newInstance(javaClassName);
            if (null != classStructure) {
                classStructures.add(classStructure);
            }
        }
        return classStructures;
    }

    // 遍历一个类结构
    private void accept(final ClassVisitor cv) {
        classReader.accept(cv, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
    }

    @Override
    public String getJavaClassName() {
        return toJavaClassName(classReader.getClassName());
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }

    private final LazyGet<ClassStructure> superClassStructureLazyGet
            = new LazyGet<ClassStructure>() {
        @Override
        protected ClassStructure initialValue() throws Throwable {
            return newInstance(toJavaClassName(classReader.getSuperName()));
        }
    };

    @Override
    public ClassStructure getSuperClassStructure() {
        return superClassStructureLazyGet.get();
    }

    private final LazyGet<List<ClassStructure>> interfaceClassStructuresLazyGet
            = new LazyGet<List<ClassStructure>>() {
        @Override
        protected List<ClassStructure> initialValue() throws Throwable {
            return newInstances(classReader.getInterfaces());
        }
    };

    @Override
    public List<ClassStructure> getInterfaceClassStructures() {
        return interfaceClassStructuresLazyGet.get();
    }

    private final LazyGet<List<ClassStructure>> annotationTypeClassStructuresLazyGet
            = new LazyGet<List<ClassStructure>>() {
        @Override
        protected List<ClassStructure> initialValue() throws Throwable {
            final List<ClassStructure> annotationTypeClassStructures = new ArrayList<ClassStructure>();
            accept(new ClassVisitor(ASM6) {

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (visible) {
                        final ClassStructure annotationTypeClassStructure = newInstance(Type.getType(desc).getClassName());
                        if (null != annotationTypeClassStructure) {
                            annotationTypeClassStructures.add(annotationTypeClassStructure);
                        }
                    }
                    return super.visitAnnotation(desc, visible);
                }

            });
            return annotationTypeClassStructures;
        }
    };

    @Override
    public List<ClassStructure> getAnnotationTypeClassStructures() {
        return annotationTypeClassStructuresLazyGet.get();
    }


    private final LazyGet<List<BehaviorStructure>> behaviorStructuresLazyGet
            = new LazyGet<List<BehaviorStructure>>() {
        @Override
        protected List<BehaviorStructure> initialValue() throws Throwable {
            final List<BehaviorStructure> behaviorStructures = new ArrayList<BehaviorStructure>();
            accept(new ClassVisitor(ASM6) {

                @Override
                public MethodVisitor visitMethod(final int access,
                                                 final String name,
                                                 final String desc,
                                                 final String signature,
                                                 final String[] exceptions) {
                    return new MethodVisitor(ASM6, super.visitMethod(access, name, desc, signature, exceptions)) {

                        private final Type methodType = Type.getMethodType(desc);
                        private final List<ClassStructure> annotationTypeClassStructures = new ArrayList<ClassStructure>();

                        private String[] typeArrayToJavaClassNameArray(final Type[] typeArray) {
                            final List<String> javaClassNames = new ArrayList<String>();
                            if (null != typeArray) {
                                for (Type type : typeArray) {
                                    javaClassNames.add(type.getClassName());
                                }
                            }
                            return javaClassNames.toArray(new String[0]);
                        }

                        private List<ClassStructure> getParameterTypeClassStructures() {
                            return newInstances(
                                    typeArrayToJavaClassNameArray(methodType.getArgumentTypes())
                            );
                        }

                        private ClassStructure getReturnTypeClassStructure() {
                            if ("<init>".equals(name)) {
                                return ClassStructureImplByAsm.this;
                            } else {
                                final Type returnType = methodType.getReturnType();
                                return newInstance(returnType.getClassName());
                            }
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (visible) {
                                final ClassStructure annotationTypeClassStructure = newInstance(Type.getType(desc).getClassName());
                                if (null != annotationTypeClassStructure) {
                                    annotationTypeClassStructures.add(annotationTypeClassStructure);
                                }
                            }
                            return super.visitAnnotation(desc, visible);
                        }

                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                            final BehaviorStructure behaviorStructure = new BehaviorStructure(
                                    new AccessImplByAsm(access),
                                    name,
                                    ClassStructureImplByAsm.this,
                                    getReturnTypeClassStructure(),
                                    getParameterTypeClassStructures(),
                                    newInstances(exceptions),
                                    annotationTypeClassStructures
                            );
                            behaviorStructures.add(behaviorStructure);
                        }
                    };
                }
            });
            return behaviorStructures;
        }
    };

    @Override
    public List<BehaviorStructure> getBehaviorStructures() {
        return behaviorStructuresLazyGet.get();
    }


    @Override
    public Access getAccess() {
        return access;
    }

}
