package com.alibaba.jvm.sandbox.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射工具类
 * Created by luanjia@taobao.com on 16/5/21.
 */
public class SandboxReflectUtils {

    /**
     * 获取Java类的方法
     * 该方法不会抛出任何声明式异常
     *
     * @param clazz               类
     * @param name                方法名
     * @param parameterClassArray 参数类型数组
     * @return Java方法
     */
    public static Method unCaughtGetClassDeclaredJavaMethod(final Class<?> clazz,
                                                            final String name,
                                                            final Class<?>... parameterClassArray) {
        try {
            return clazz.getDeclaredMethod(name, parameterClassArray);
        } catch (NoSuchMethodException e) {
            throw new UnCaughtException(e);
        }
    }

    public static <T> T unCaughtInvokeMethod(final Method method,
                                             final Object target,
                                             final Object... parameterArray) {
        final boolean isAccessible = method.isAccessible();
        try {
            method.setAccessible(true);
            return (T) method.invoke(target, parameterArray);
        } catch (Throwable e) {
            throw new UnCaughtException(e);
        } finally {
            method.setAccessible(isAccessible);
        }
    }

    public static Field unCaughtGetClassDeclaredJavaField(final Class<?> clazz,
                                                          final String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new UnCaughtException(e);
        }
    }

    public static <T> T unCaughtGetClassDeclaredJavaFieldValue(final Class<?> clazz,
                                                               final String name,
                                                               final Object target) {
        final Field field = unCaughtGetClassDeclaredJavaField(clazz, name);
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new UnCaughtException(e);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    public static void unCaughtSetClassDeclaredJavaFieldValue(final Class<?> clazz,
                                                              final String name,
                                                              final Object target,
                                                              final Object value) {
        final Field field = unCaughtGetClassDeclaredJavaField(clazz, name);
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new UnCaughtException(e);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 定义类
     *
     * @param loader         目标ClassLoader
     * @param javaClassName  类名称
     * @param classByteArray 类字节码数组
     * @return 定义的类
     * @throws InvocationTargetException 目标方法调用发生异常
     * @throws IllegalAccessException    目标方法不可进入
     */
    public static Class<?> defineClass(final ClassLoader loader,
                                       final String javaClassName,
                                       final byte[] classByteArray) throws InvocationTargetException, IllegalAccessException {

        final Method defineClassMethod =
                unCaughtGetClassDeclaredJavaMethod(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class);

        synchronized (defineClassMethod) {
            final boolean acc = defineClassMethod.isAccessible();
            try {
                defineClassMethod.setAccessible(true);
                return (Class<?>) defineClassMethod.invoke(
                        loader,
                        javaClassName,
                        classByteArray,
                        0,
                        classByteArray.length
                );
            } finally {
                defineClassMethod.setAccessible(acc);
            }
        }

    }

}
