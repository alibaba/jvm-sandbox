package com.alibaba.jvm.sandbox.core.util;

import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureFactory;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;

/**
 * ASM工具集
 *
 * @author luanjia@taobao.com
 */
public class AsmUtils {

    /**
     * 获取两个类型的共同父类
     * just the same
     * {@code org.objectweb.asm.ClassWriter#getCommonSuperClass(String, String)}
     *
     * @param type1  类型1
     * @param type2  类型2
     * @param loader 所在ClassLoader
     * @return 共同的父类
     */
    public static String getCommonSuperClass(String type1, String type2, ClassLoader loader) {
        return getCommonSuperClassImplByAsm(type1, type2, loader);
    }

    // implements by ASM
    private static String getCommonSuperClassImplByAsm(String type1, String type2, ClassLoader targetClassLoader) {
        // JDK 9+: 优先使用Class.forName方式获取类层级，避免模块化下资源加载失败
        try {
            Class<?> class1 = getClassForName(type1, targetClassLoader);
            Class<?> class2 = getClassForName(type2, targetClassLoader);
            if (class1 != null && class2 != null) {
                return getCommonSuperClassByReflection(class1, class2);
            }
        } catch (Throwable e) {
            // fall through to resource-based approach
        }

        // 回退到基于资源加载的方式（JDK 8兼容）
        return getCommonSuperClassByResource(type1, type2, targetClassLoader);
    }

    private static Class<?> getClassForName(String type, ClassLoader loader) {
        String javaClassName = type.replace('/', '.');
        ClassLoader cl = loader != null ? loader : ClassLoader.getSystemClassLoader();
        try {
            return Class.forName(javaClassName, false, cl);
        } catch (Throwable e) {
            return null;
        }
    }

    private static String getCommonSuperClassByReflection(Class<?> class1, Class<?> class2) {
        if (class2.isAssignableFrom(class1)) {
            return class2.getName().replace('.', '/');
        }
        if (class1.isAssignableFrom(class2)) {
            return class1.getName().replace('.', '/');
        }
        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        }
        Class<?> superClass = class1.getSuperclass();
        while (superClass != null) {
            if (superClass.isAssignableFrom(class2)) {
                return superClass.getName().replace('.', '/');
            }
            superClass = superClass.getSuperclass();
        }
        return "java/lang/Object";
    }

    private static String getCommonSuperClassByResource(String type1, String type2, ClassLoader targetClassLoader) {
        InputStream inputStreamOfType1 = null, inputStreamOfType2 = null;
        try {
            //targetClassLoader 为null，说明是BootStrapClassLoader，不能显式引用，故使用系统类加载器间接引用
            if (null == targetClassLoader) {
                targetClassLoader = ClassLoader.getSystemClassLoader();
            }
            if (null == targetClassLoader) {
                return "java/lang/Object";
            }
            inputStreamOfType1 = targetClassLoader.getResourceAsStream(type1 + ".class");
            if (null == inputStreamOfType1) {
                return "java/lang/Object";
            }
            inputStreamOfType2 = targetClassLoader.getResourceAsStream(type2 + ".class");
            if (null == inputStreamOfType2) {
                return "java/lang/Object";
            }
            final ClassStructure classStructureOfType1 = ClassStructureFactory.createClassStructure(inputStreamOfType1, targetClassLoader);
            final ClassStructure classStructureOfType2 = ClassStructureFactory.createClassStructure(inputStreamOfType2, targetClassLoader);
            if (classStructureOfType2.getFamilyTypeClassStructures().contains(classStructureOfType1)) {
                return type1;
            }
            if (classStructureOfType1.getFamilyTypeClassStructures().contains(classStructureOfType2)) {
                return type2;
            }
            if (classStructureOfType1.getAccess().isInterface()
                    || classStructureOfType2.getAccess().isInterface()) {
                return "java/lang/Object";
            }
            ClassStructure classStructure = classStructureOfType1;
            do {
                classStructure = classStructure.getSuperClassStructure();
                if (null == classStructure) {
                    return "java/lang/Object";
                }
            } while (!classStructureOfType2.getFamilyTypeClassStructures().contains(classStructure));
            return toInternalClassName(classStructure.getJavaClassName());
        } catch (Throwable e) {
            return "java/lang/Object";
        } finally {
            IOUtils.closeQuietly(inputStreamOfType1);
            IOUtils.closeQuietly(inputStreamOfType2);
        }
    }

}
