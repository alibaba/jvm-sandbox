package com.alibaba.jvm.sandbox.api.spi;

/**
 * 模块Jar包生命周期管理
 *
 * @author oldmanpushcart@gmail.com
 * @since {@code sandbox-api:1.2.0}
 */
public interface ModuleJarLifeCycleProvider {

    /**
     * 模块Jar文件卸载完所有模块后，正式卸载Jar文件之前之后调用！
     */
    void onJarUnLoadCompleted();

}
