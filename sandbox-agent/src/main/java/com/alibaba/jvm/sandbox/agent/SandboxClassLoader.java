package com.alibaba.jvm.sandbox.agent;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

/**
 * 加载Sandbox用的ClassLoader
 * Created by luanjia@taobao.com on 2016/10/26.
 */
class SandboxClassLoader extends URLClassLoader {

    private final String namespace;
    private final String path;

    SandboxClassLoader(final String namespace,
                       final String sandboxCoreJarFilePath) throws MalformedURLException {
        super(new URL[]{new URL("file:" + sandboxCoreJarFilePath)});
        this.namespace = namespace;
        this.path = sandboxCoreJarFilePath;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if(null != url) {
            return url;
        }
        url = super.getResource(name);
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = findResources(name);
        if( null != urls ) {
            return urls;
        }
        urls = super.getResources(name);
        return urls;
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

    @Override
    public String toString() {
        return String.format("SandboxClassLoader[namespace=%s;path=%s;]", namespace, path);
    }


    /**
     * 尽可能关闭ClassLoader
     * <p>
     * URLClassLoader会打开指定的URL资源，在SANDBOX中则是对应的Jar文件，如果不在shutdown的时候关闭ClassLoader，会导致下次再次加载
     * 的时候，依然会访问到上次所打开的文件（底层被缓存起来了）
     * <p>
     * 在JDK1.7版本中，URLClassLoader提供了{@code close()}方法来完成这件事；但在JDK1.6版本就要下点手段了；
     * <p>
     * 该方法将会被{@code ControlModule#shutdown}通过反射调用，
     * 请保持方法声明一致
     */
    @SuppressWarnings("unused")
    public void closeIfPossible() {
        // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
        try {
            Class    clazz   = URLClassLoader.class;
            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                if (method.getName().equals("close")) {
                    method.invoke(this);
                    return;
                }
            }

            // 如果不能直接通过URLClassLoader的close方法关闭那么就需要反向查找所有已经打开了的jar文件并关闭了,
            // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
            // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
            Field ucpField = clazz.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(this);

            Field loadersField = ucp.getClass().getDeclaredField("loaders");
            loadersField.setAccessible(true);
            List loaders = (List) loadersField.get(ucp);

            for (Object loader : loaders) {
                Class  jarLoaderClass = loader.getClass();
                Method method         = jarLoaderClass.getDeclaredMethod("getJarFile");
                method.setAccessible(true);

                // 释放jar文件连接
                JarFile jarFile = (JarFile) method.invoke(loader);
                jarFile.close();
            }
        } catch (Throwable t) {
            // ignore...
        }
    }

}
