package com.alibaba.jvm.sandbox.api.resource;

/**
 * {@link JniAnchorManager}
 * <p>
 *
 * jni锚点服务，基于官方JNI接口，实现一些运行时扩展能力
 * <p>
 * 参考文档：
 * <p>
 * <a href="https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html">jvmti</a>
 *
 * @author zhaoyb1990
 */
public interface JniAnchorManager {

    /**
     * 获取当前jvm中的目标类实例
     *
     * @param klass 目标类
     * @param limit 限制返回数量
     * @param <T>   泛型
     * @return 目标类实例数组
     */
    <T> T[] getInstances(Class<T> klass, int limit);

    /**
     * 获取当前jvm中的目标类实例
     *
     * @param klass 目标类
     * @param <T>   泛型
     * @return 目标类实例数组
     */
    <T> T[] getInstances(Class<T> klass);
}
