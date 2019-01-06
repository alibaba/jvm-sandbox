package com.alibaba.jvm.sandbox.module.debug;

import ch.qos.logback.classic.LoggerContext;
import com.alibaba.jvm.sandbox.api.spi.ModuleJarLifeCycleProvider;
import org.kohsuke.MetaInfServices;
import org.slf4j.LoggerFactory;

@MetaInfServices(ModuleJarLifeCycleProvider.class)
public class OnJarUnLoadCompleted implements ModuleJarLifeCycleProvider {

    @Override
    public ClassLoader waitingFor(ClassLoader[] loaded, ClassLoader inComing) {
        return null;
    }

    @Override
    public void onJarUnLoadCompleted() {
        closeLogback();
    }

    // 关闭Logback日志框架
    private void closeLogback() {
        try {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        } catch (Throwable cause) {
            cause.printStackTrace();
        }
    }

}