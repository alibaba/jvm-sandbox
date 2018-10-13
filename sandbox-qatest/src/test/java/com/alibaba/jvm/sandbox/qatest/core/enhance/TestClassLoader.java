package com.alibaba.jvm.sandbox.qatest.core.enhance;

/**
 * 一个破坏了双亲委派的ClassLoader，用于隔离测试类
 */
public class TestClassLoader extends ClassLoader {

    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            return super.loadClass(name, resolve);
        }
    }

}