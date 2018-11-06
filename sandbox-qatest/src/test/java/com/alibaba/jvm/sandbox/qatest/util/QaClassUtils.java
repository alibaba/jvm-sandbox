package com.alibaba.jvm.sandbox.qatest.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;

public class QaClassUtils {

    /**
     * 目标Class文件转换为字节码数组
     *
     * @param targetClass 目标Class文件
     * @return 目标Class文件字节码数组
     * @throws IOException 转换出错
     */
    public static byte[] toByteArray(final Class<?> targetClass) throws IOException {
        final InputStream is = targetClass.getClassLoader().getResourceAsStream(toResourceName(targetClass.getName()));
        try {
            return IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static String toResourceName(String javaClassName) {
        return toInternalClassName(javaClassName).concat(".class");
    }

    public static Set<Class<?>> getJdkFamilySuperClasses(Class<?> clazz) {
        final Set<Class<?>> familySuperClasses = new LinkedHashSet<Class<?>>();
        for (Class<?> superClass = clazz.getSuperclass();
             superClass != null;
             superClass = superClass.getSuperclass()) {
            familySuperClasses.add(superClass);
        }
        return familySuperClasses;
    }

    public static Set<Class<?>> getJdkClassInterfaces(Class<?> clazz) {
        final Set<Class<?>> interfaceClasses = new LinkedHashSet<Class<?>>();
        if (ArrayUtils.isNotEmpty(clazz.getInterfaces())) {
            for (Class<?> interfaceClass : clazz.getInterfaces()) {
                interfaceClasses.add(interfaceClass);
                interfaceClasses.addAll(getJdkFamilySuperClasses(interfaceClass));
            }
        }
        return interfaceClasses;
    }

    public static Set<Class<?>> getJdkFamilyClassInterface(Class<?> clazz) {
        final Set<Class<?>> familyInterfaceClasses = new LinkedHashSet<Class<?>>();
        for (final Class<?> interfaceClass : getJdkClassInterfaces(clazz)) {
            familyInterfaceClasses.add(interfaceClass);
            familyInterfaceClasses.addAll(getJdkClassInterfaces(interfaceClass));
        }
        return familyInterfaceClasses;
    }

    /**
     * 获取类的所有类型
     * <p>
     * <li>所有父类</li>
     * <li>所有父类的接口及这些接口的所有父类</li>
     * <li>所有接口的父类</li>
     * </p>
     *
     * @param clazz
     * @return
     */
    public static Set<Class<?>> getJdkFamilyClassType(Class<?> clazz) {

        // 获取所有的父类
        final Set<Class<?>> familyTypes = new LinkedHashSet<Class<?>>(getJdkFamilyClassInterface(clazz));

        for (final Class<?> superClass : getJdkFamilySuperClasses(clazz)) {
            familyTypes.add(superClass);
            familyTypes.addAll(getJdkFamilyClassInterface(superClass));
        }

        // 递归获取所有接口及其父类
        return familyTypes;
    }

    public static Set<Class<?>> getJdkAnnotationType(Class<?> clazz) {
        final Set<Class<?>> annotationClasses = new LinkedHashSet<Class<?>>();
        if(ArrayUtils.isNotEmpty(clazz.getAnnotations())) {
            for (final Annotation annotation : clazz.getAnnotations()) {
                if (annotation.getClass().isAnnotation()) {
                    annotationClasses.add(annotation.getClass());
                }
                for (final Class annotationInterfaceClass : annotation.getClass().getInterfaces()) {
                    if (annotationInterfaceClass.isAnnotation()) {
                        annotationClasses.add(annotationInterfaceClass);
                    }
                }
            }
        }
        return annotationClasses;
    }



}
