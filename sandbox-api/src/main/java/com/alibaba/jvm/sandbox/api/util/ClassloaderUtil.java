package com.alibaba.jvm.sandbox.api.util;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ClassloaderUtil}
 * <p>
 * 类加载器工具
 *
 * @author zhaoyb1990
 */
public class ClassloaderUtil {

    private final static String PANDORA_CLASSLOADER = "com.taobao.pandora.service.loader.ModuleClassLoader";

    private final static Set<String> BIZ_CLASS_LOADERS = new HashSet<>();

    static {
        // tomcat
        BIZ_CLASS_LOADERS.add("org.apache.catalina.loader.ParallelWebappClassLoader");
        // spring boot
        BIZ_CLASS_LOADERS.add("org.springframework.boot.loader.LaunchedURLClassLoader");
        // pandora boot
        BIZ_CLASS_LOADERS.add("com.taobao.pandora.boot.loader.LaunchedURLClassLoader");
    }

    /**
     * 包装类加载器名称
     *
     * @param classLoader 类加载器
     * @return 类加载器名称
     */
    public static String wrapperName(ClassLoader classLoader) {
        if (classLoader == null) {
            return "BootstrapClassLoader";
        }
        String name = classLoader.getClass().getName();
        if (PANDORA_CLASSLOADER.equals(name)) {
            return classLoader.toString();
        }
        return name;
    }

    /**
     * 查询当前应用的容器类加载器
     *
     * @param classLoaders 类加载器
     * @return true / false
     */
    public static ClassLoader findLaunchedClassLoader(Set<ClassLoader> classLoaders) {
        for (ClassLoader classLoader : classLoaders) {
            if (BIZ_CLASS_LOADERS.contains(wrapperName(classLoader))) {
                return classLoader;
            }
        }
        return null;
    }
}