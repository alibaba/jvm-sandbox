package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 已加载类数据源默认实现
 * Created by luanjia on 2017/4/2.
 */
public class DefaultLoadedClassDataSource implements LoadedClassDataSource {

    private final Instrumentation inst;

    public DefaultLoadedClassDataSource(Instrumentation inst) {
        this.inst = inst;
    }

    public Set<Class<?>> list() {
        final Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            classes.add(clazz);
        }
        return classes;
    }

    private String[] toJavaClassNameArray(final Class<?>[] classArray) {
        final String[] javaClassNameArray = new String[classArray.length];
        for (int index = 0; index < classArray.length; index++) {
            javaClassNameArray[index] = classArray[index].getName();
        }
        return javaClassNameArray;
    }

    private String[] toJavaClassNameArray(final Annotation[] annotationArray) {
        final String[] javaClassNameArray = new String[annotationArray.length];
        for (int index = 0; index < annotationArray.length; index++) {
            javaClassNameArray[index] = annotationArray[index].getClass().getName();
        }
        return javaClassNameArray;
    }

    private boolean doMethodFilter(final Filter filter, Class<?> clazz) {
        if (null == filter) {
            return false;
        }

        // 寻找声明的方法
        final Method[] declaredMethodArray = clazz.getDeclaredMethods();
        if (null != declaredMethodArray
                && declaredMethodArray.length > 0) {
            for (Method declaredMethod : declaredMethodArray) {
                if (filter.doMethodFilter(
                        declaredMethod.getModifiers(),
                        declaredMethod.getName(),
                        toJavaClassNameArray(declaredMethod.getParameterTypes()),
                        toJavaClassNameArray(declaredMethod.getExceptionTypes()),
                        toJavaClassNameArray(declaredMethod.getAnnotations())
                )) {
                    return true;
                }
            }
        }


        // 寻找声明的构造函数
        final Constructor[] declaredConstructorArray = clazz.getDeclaredConstructors();
        if (null != declaredConstructorArray
                && declaredConstructorArray.length > 0) {
            for (Constructor declaredConstructor : declaredConstructorArray) {
                if (filter.doMethodFilter(
                        declaredConstructor.getModifiers(),
                        "<init>",
                        toJavaClassNameArray(declaredConstructor.getParameterTypes()),
                        toJavaClassNameArray(declaredConstructor.getExceptionTypes()),
                        toJavaClassNameArray(declaredConstructor.getAnnotations())
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 根据过滤器搜索出匹配的类集合
     *
     * @param filter 扩展过滤器
     * @return 匹配的类集合
     */
    public Set<Class<?>> find(Filter filter) {
        final Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        if (null == filter) {
            return classes;
        }
        for (Class<?> clazz : list()) {

            try {

                final String superClassJavaClassName;
                if (null == clazz.getSuperclass()) {
                    superClassJavaClassName = null;
                } else {
                    superClassJavaClassName = clazz.getSuperclass().getName();
                }

                if (filter.doClassFilter(
                        clazz.getModifiers(),
                        clazz.getName(),
                        superClassJavaClassName,
                        toJavaClassNameArray(clazz.getInterfaces()),
                        toJavaClassNameArray(clazz.getAnnotations()))
                        && doMethodFilter(filter, clazz)) {
                    classes.add(clazz);
                }

            } catch (Throwable cause) {

                // 在这里可能会遇到非常坑爹的模块卸载错误
                // 当一个URLClassLoader被动态关闭之后，但JVM已经加载的类并不知情（因为没有GC）
                // 所以当尝试获取这个类更多详细信息的时候会引起关联类的ClassNotFoundException等未知的错误（取决于底层ClassLoader的实现）
                // 这里没有办法穷举出所有的异常情况，所以catch Throwable来完成异常容灾处理
                // 当解析类出现异常的时候，直接简单粗暴的认为根本没有这个类就好了

            }


        }
        return classes;
    }

}
