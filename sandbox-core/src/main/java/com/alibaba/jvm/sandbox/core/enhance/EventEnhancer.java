package com.alibaba.jvm.sandbox.core.enhance;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.core.enhance.weaver.asm.EventWeaver;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;
import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toJavaClassName;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * 事件代码增强器
 * Created by luanjia@taobao.com on 16/7/12.
 */
public class EventEnhancer implements Enhancer {

    private static final Logger logger = LoggerFactory.getLogger(EventEnhancer.class);

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
                    return toInternalClassName(c.getName());
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
                                final Set<String> signCodes,
                                final int listenerId,
                                final Event.Type[] eventTypeArray) {
        final ClassReader cr = new ClassReader(sourceByteCodeArray);
        final ClassWriter cw = createClassWriter(targetClassLoader, cr);
        final int targetClassLoaderObjectID = ObjectIDs.instance.identity(targetClassLoader);
        cr.accept(
                new EventWeaver(
                        Opcodes.ASM6, cw, listenerId,
                        targetClassLoaderObjectID,
                        cr.getClassName(),
                        signCodes,
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
                                  final byte[] byteCodeArray,
                                  final Set<String> signCodes,
                                  final int listenerId,
                                  final Event.Type[] eventTypeArray) {
        // 如果定义间谍类失败了,则后续不需要增强
        try {
            SpyUtils.init();
            // defineSpyIfNecessary(targetClassLoader);
        } catch (Throwable cause) {
            logger.warn("define Spy to target ClassLoader={} failed.", targetClassLoader, cause);
            return byteCodeArray;
        }

        // 返回增强后字节码
        return weavingEvent(targetClassLoader, byteCodeArray, signCodes, listenerId, eventTypeArray);
    }

}
