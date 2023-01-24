package com.alibaba.jvm.sandbox.api;

/**
 * 用于区分JDK9之后出现的{@code java.lang.Module}，不然代码在JDK9+的JVM上跑会报错
 * @since {@code sandbox-common-api:1.4.0}
 */
public interface SandboxModule extends com.alibaba.jvm.sandbox.api.Module {
}
