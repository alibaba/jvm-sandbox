package com.alibaba.jvm.sandbox.agent;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 加载Sandbox用的ClassLoader
 * Created by luanjia@taobao.com on 2016/10/26.
 */
class SandboxClassLoader extends URLClassLoader {

    SandboxClassLoader(final String sandboxCoreJarFilePath) throws MalformedURLException {
        super(new URL[]{new URL("file:" + sandboxCoreJarFilePath)});
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

//        // 优先从parent（SystemClassLoader）里加载系统类，避免抛出ClassNotFoundException
//        if(name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
//            return super.loadClass(name, resolve);
//        }

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
