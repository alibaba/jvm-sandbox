package com.alibaba.jvm.sandbox.core.classloader;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 类加载锁
 */
class ClassLoadingLock {

    private final ConcurrentHashMap<String, Object> classLoadingLockMap = new ConcurrentHashMap<String, Object>();

    /**
     * 类加载
     */
    interface ClassLoading {

        /**
         * 加载类
         *
         * @param javaClassName 类名称
         * @return 类
         * @throws ClassNotFoundException 类找不到
         */
        Class<?> loadClass(String javaClassName) throws ClassNotFoundException;

    }

    /**
     * <p>解决 #218</p>
     * 参考JDK1.7；因为sandbox需要维持在1.6上运行，
     * 未来平台统一升级到1.8+之后就用系统自带的
     *
     * @param javaClassName JavaClassName
     * @return ClassLoading Lock
     */
    private Object getClassLoadingLock(String javaClassName) {
        final Object newLock = new Object();
        final Object lock = classLoadingLockMap.putIfAbsent(javaClassName, newLock);
        return null == lock
                ? newLock
                : lock;
    }

    /**
     * 在锁中加载类
     *
     * @param javaClassName JavaClassName
     * @param loading       类加载回调(在锁中)
     * @return 加载的类
     * @throws ClassNotFoundException 类找不到
     */
    public Class<?> loadingInLock(String javaClassName, ClassLoading loading) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(javaClassName)) {
            try {
                return loading.loadClass(javaClassName);
            } finally {
                classLoadingLockMap.remove(javaClassName);
            }
        }
    }

}
