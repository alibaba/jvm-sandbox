package com.alibaba.jvm.sandbox.core.enhance;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.core.classloader.ModuleClassLoader;
import com.alibaba.jvm.sandbox.core.classloader.ProviderClassLoader;
import com.alibaba.jvm.sandbox.core.enhance.weaver.asm.EventWeaver;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;
import com.alibaba.jvm.sandbox.util.SandboxStringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.jvm.sandbox.util.SandboxStringUtils.toJavaClassName;
import static com.alibaba.jvm.sandbox.util.SandboxStringUtils.toJavaClassNameArray;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * 事件代码增强器
 * Created by luanjia@taobao.com on 16/7/12.
 */
public class EventEnhancer implements Enhancer {

    private static final Logger logger = LoggerFactory.getLogger(EventEnhancer.class);

    // 类&方法过滤器(过滤目标类和方法是否参与到本次增强中)
    private final Filter filter;

    private final int listenerId;

    private final String uniqueCodePrefix;
    private final Set<String> affectMethodUniqueSet;
    private final boolean isEnableUnsafe;
    private final Event.Type[] eventTypeArray;

    public EventEnhancer(final int listenerId,
                         final Filter filter,
                         final String uniqueCodePrefix,
                         final Set<String> affectMethodUniqueSet,
                         final boolean isEnableUnsafe,
                         final Event.Type[] eventTypeArray) {
        this.filter = filter;
        this.listenerId = listenerId;
        this.uniqueCodePrefix = uniqueCodePrefix;
        this.affectMethodUniqueSet = affectMethodUniqueSet;
        this.isEnableUnsafe = isEnableUnsafe;
        this.eventTypeArray = eventTypeArray;
    }

//    /*
//     * 在目标ClassLoader中尝试定义间谍类(Spy.class)
//     */
//    private void defineSpyIfNecessary(final ClassLoader targetClassLoader) throws IOException, InvocationTargetException, IllegalAccessException {
//
//        final String spyClassName = Spy.class.getName();
//        final String retInSpyClassName = Spy.Ret.class.getName();
//        final String selfCallBarrierInSpyClassName = Spy.SelfCallBarrier.class.getName();
//        final String nodeInSelfCallBarrierInSpyClassName = Spy.SelfCallBarrier.Node.class.getName();
//
//        // 从目标ClassLoader中尝试加载或定义ClassLoader
//        Class<?> spyClassFromTargetClassLoader = null;
//        try {
//
//            // 如果对方是bootstrap就算了
//            if (null == targetClassLoader) {
//                logger.debug("target ClassLoader is BootstrapClassLoader, ignore define Spy.");
//                spyClassFromTargetClassLoader = Spy.class;
//                return;
//            }
//
//            // 去目标类加载器中找下是否已经存在间谍
//            // 如果间谍已经存在就算了
//            spyClassFromTargetClassLoader = targetClassLoader.loadClass(spyClassName);
//            logger.debug("target ClassLoader={} was already have Spy, ignore define.", targetClassLoader);
//
//        }
//
//        // 看来间谍不存在啊
//        catch (ClassNotFoundException cnfe) {
//
//            logger.debug("target ClassLoader={} was not have Spy yet, prepare define Spy.", targetClassLoader);
//
//            try {
//                // 在目标类加载起中混入间谍
//                SandboxReflectUtils.defineClass(
//                        targetClassLoader,
//                        retInSpyClassName,
//                        toByteArray(EventEnhancer.class.getResourceAsStream("/" + retInSpyClassName.replace('.', '/') + ".class"))
//                );
//                SandboxReflectUtils.defineClass(
//                        targetClassLoader,
//                        selfCallBarrierInSpyClassName,
//                        toByteArray(EventEnhancer.class.getResourceAsStream("/" + selfCallBarrierInSpyClassName.replace('.', '/') + ".class"))
//                );
//                SandboxReflectUtils.defineClass(
//                        targetClassLoader,
//                        nodeInSelfCallBarrierInSpyClassName,
//                        toByteArray(EventEnhancer.class.getResourceAsStream("/" + nodeInSelfCallBarrierInSpyClassName.replace('.', '/') + ".class"))
//                );
//                spyClassFromTargetClassLoader = SandboxReflectUtils.defineClass(
//                        targetClassLoader,
//                        spyClassName,
//                        toByteArray(EventEnhancer.class.getResourceAsStream("/" + spyClassName.replace('.', '/') + ".class"))
//                );
//                logger.info("define Spy to target ClassLoader={} success.", targetClassLoader);
//            } catch (InvocationTargetException ite) {
//                if (ite.getTargetException() instanceof LinkageError) {
//                    // CloudEngine 由于 loadClass 不到,会导致
//                    // java.lang.LinkageError: targetClassLoader
//                    // (instance of  com/alipay/cloudengine/extensions/equinox/KernelAceClassLoader): attempted
//                    // duplicate class definition for name: "com/taobao/arthas/core/advisor/Spy"
//                    // 这里尝试忽略
//                    logger.debug("define Spy to target ClassLoader={} failed, but was LinkageError, ignore this error.", targetClassLoader, ite);
//                } else {
//                    throw ite;
//                }
//            }
//
//        }
//
//        // 无论从哪里取到spyClass，都需要重新初始化一次
//        // 用以兼容重新加载的场景
//        // 当然，这样做会给渲染的过程带来一定的性能开销，不过能简化编码复杂度
//        finally {
//
//            if (null != spyClassFromTargetClassLoader) {
//                // 初始化间谍
//                try {
//                    invokeStaticMethod(
//                            spyClassFromTargetClassLoader,
//                            "init",
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onBefore",
//                                    int.class,
//                                    int.class,
//                                    Class.class,
//                                    String.class,
//                                    String.class,
//                                    String.class,
//                                    Object.class,
//                                    Object[].class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onReturn",
//                                    int.class,
//                                    Class.class,
//                                    Object.class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onThrows",
//                                    int.class,
//                                    Class.class,
//                                    Throwable.class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onLine",
//                                    int.class,
//                                    int.class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallBefore",
//                                    int.class,
//                                    int.class,
//                                    String.class,
//                                    String.class,
//                                    String.class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallReturn",
//                                    int.class
//                            ),
//                            unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallThrows",
//                                    int.class,
//                                    String.class
//                            )
//                    );
//                } catch (Throwable throwable) {
//                    logger.warn("Spy.init method invoke failed, is impossible!!!", throwable);
//                }
//            }
//
//        }
//
//    }


    private boolean isInterface(int access) {
        return (Opcodes.ACC_INTERFACE & access) == Opcodes.ACC_INTERFACE;
    }

    private boolean isAnnotation(int access) {
        return (Opcodes.ACC_ANNOTATION & access) == Opcodes.ACC_ANNOTATION;
    }

    private boolean isEnum(int access) {
        return (Opcodes.ACC_ENUM & access) == Opcodes.ACC_ENUM;
    }

    private boolean isNative(int access) {
        return (Opcodes.ACC_NATIVE & access) == Opcodes.ACC_NATIVE;
    }

    /**
     * 是否需要忽略的access
     *
     * @param access class's access
     * @return TRUE:需要被忽略；FALSE:不能被忽略
     */
    private boolean isIgnoreAccess(final int access) {

        // 接口就没有必要增强了吧
        return isInterface(access)

                // annotation目前没有增强的必要，因为一眼就能看出答案
                || isAnnotation(access)

                // 枚举类也没有增强的必要，也是一眼就能看出答案
                || isEnum(access)

                // Native的方法暂时不支持
                || isNative(access);
    }

    /**
     * 是否需要忽略的ClassLoader
     *
     * @param loader 目标加载类的ClassLoader
     * @return TRUE:需要被忽略；FALSE:不能被忽略
     */
    private boolean isIgnoreClassLoader(final ClassLoader loader) {

        // 如果沙箱没有开启Unsafe模式，是不允许增强BootStrapClassLoader的类
        return (!isEnableUnsafe && null == loader)

                // 不允许增强沙箱自己的类
                || loader == getClass().getClassLoader()

                // 不允许增强来自模块的类
                || loader instanceof ModuleClassLoader

                // 不允许增强来自服务提供库的类
                || loader instanceof ProviderClassLoader;

    }

    /**
     * 是否需要忽略增强的类
     *
     * @param cr                ClassReader
     * @param targetClassLoader 目标ClassLoader
     * @return true:忽略增强;false:需要增强
     */
    private boolean isIgnoreClass(final ClassReader cr,
                                  final ClassLoader targetClassLoader) {
        final String[] annotationTypeArray = null;
        final int access = cr.getAccess();

        // 对一些需要忽略的access进行过滤
        return isIgnoreAccess(access)

                // 对一系列的ClassLoader进行过滤
                || isIgnoreClassLoader(targetClassLoader)

                // 如果目标类的前缀是Sandbox的前缀，则放弃
                || StringUtils.startsWith(toJavaClassName(cr.getClassName()), "com.alibaba.jvm.sandbox.")

                // 如果目标类是Lambda表达式生成的临时类，则放弃
                || StringUtils.contains(toJavaClassName(cr.getClassName()), "$$Lambda$")

                // 按照Filter#doClassFilter()完成过滤
                || !filter.doClassFilter(access,
                toJavaClassName(cr.getClassName()),
                toJavaClassName(cr.getSuperName()),
                toJavaClassNameArray(cr.getInterfaces()),
                toJavaClassNameArray(annotationTypeArray));

    }

    /**
     * 创建ClassWriter for asm
     *
     * @param cr ClassReader
     * @return ClassWriter
     */
    private ClassWriter createClassWriter(final ClassLoader targetClassLoader,
                                          final ClassReader cr) {
        return new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {

            /*
             * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
             * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
             * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
             * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
             *
             * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
             * 规避了原有代码采用Object.class.getClassLoader()的方式
             */
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                Class<?> c, d;
                try {
                    c = Class.forName(toJavaClassName(type1), false, targetClassLoader);
                    d = Class.forName(toJavaClassName(type2), false, targetClassLoader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (c.isAssignableFrom(d)) {
                    return type1;
                }
                if (d.isAssignableFrom(c)) {
                    return type2;
                }
                if (c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return SandboxStringUtils.toInternalClassName(c.getName());
                }
            }

        };
    }

    /**
     * 编织事件方法
     *
     * @param sourceByteCodeArray 原始字节码数组
     * @return 编织后的字节码数组
     */
    private byte[] weavingEvent(final ClassLoader targetClassLoader,
                                final byte[] sourceByteCodeArray,
                                final AtomicBoolean reWriteMark) {
        final ClassReader cr = new ClassReader(sourceByteCodeArray);
        final ClassWriter cw = createClassWriter(targetClassLoader, cr);
        final int targetClassLoaderObjectID = ObjectIDs.instance.identity(targetClassLoader);
        cr.accept(
                new EventWeaver(
                        Opcodes.ASM5, cw, listenerId,
                        targetClassLoaderObjectID,
                        cr.getClassName(),
                        filter,
                        reWriteMark,
                        uniqueCodePrefix,
                        affectMethodUniqueSet,
                        eventTypeArray
                ),
                EXPAND_FRAMES
        );
        return cw.toByteArray();
        // return dumpClassIfNecessary(SandboxStringUtils.toJavaClassName(cr.getClassName()), cw.toByteArray());
    }


//    /*
//     * dump class to file
//     * 用于代码调试
//     */
//    private static byte[] dumpClassIfNecessary(String className, byte[] data) {
//        final File dumpClassFile = new File("./sandbox-class-dump/" + className + ".class");
//        final File classPath = new File(dumpClassFile.getParent());
//
//        // 创建类所在的包路径
//        if (!classPath.mkdirs()
//                && !classPath.exists()) {
//            logger.warn("create dump classpath={} failed.", classPath);
//            return data;
//        }
//
//        // 将类字节码写入文件
//        try {
//            writeByteArrayToFile(dumpClassFile, data);
//            logger.info("dump {} to {} success.", className, dumpClassFile);
//        } catch (IOException e) {
//            logger.warn("dump {} to {} failed.", className, dumpClassFile, e);
//        }
//
//        return data;
//    }

    @Override
    public byte[] toByteCodeArray(final ClassLoader targetClassLoader,
                                  final byte[] byteCodeArray) {

        final AtomicBoolean reWriteMark = new AtomicBoolean(false);
        final ClassReader cr = new ClassReader(byteCodeArray);

        // 如果目标对象不在类匹配范围,则主动忽略增强
        if (isIgnoreClass(cr, targetClassLoader)) {
            logger.debug("class={} is ignore by filter, return origin bytecode", cr.getClassName());
            return byteCodeArray;
        }
        logger.debug("class={} is matched filter, prepare to enhance.", cr.getClassName());

        // 如果定义间谍类失败了,则后续不需要增强
        try {
            SpyUtils.init();
            // defineSpyIfNecessary(targetClassLoader);
        } catch (Throwable cause) {
            logger.warn("define Spy to target ClassLoader={} failed. class={}", targetClassLoader, cr.getClassName(), cause);
            return byteCodeArray;
        }

        // 返回增强后字节码
        final byte[] returnByteCodeArray = weavingEvent(targetClassLoader, byteCodeArray, reWriteMark);
        logger.debug("enhance class={} success, before bytecode.size={}; after bytecode.size={}; reWriteMark={}",
                cr.getClassName(),
                ArrayUtils.getLength(byteCodeArray),
                ArrayUtils.getLength(returnByteCodeArray),
                reWriteMark.get()
        );
        if (reWriteMark.get()) {
            return returnByteCodeArray;
        } else {
            return byteCodeArray;
        }
    }

}
