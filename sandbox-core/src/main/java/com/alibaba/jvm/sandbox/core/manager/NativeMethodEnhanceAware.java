package com.alibaba.jvm.sandbox.core.manager;

/**
 * 本地方法前缀
 *
 * {@link java.lang.instrument.Instrumentation}
 *
 * @author zhuangpeng
 * @since 2020/9/13
 */
public interface NativeMethodEnhanceAware {
    /**
     * 获取本地方法前置
     */
    String getNativeMethodPrefix();

    void markNativeMethodEnhance();
}
