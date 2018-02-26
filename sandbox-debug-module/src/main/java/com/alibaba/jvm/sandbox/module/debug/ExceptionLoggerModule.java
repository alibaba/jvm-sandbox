package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * 异常类创建日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "debug-exception-logger", version = "0.0.1", author = "luanjia@taobao.com")
public class ExceptionLoggerModule implements Module, LoadCompleted {

    private final Logger exLogger = LoggerFactory.getLogger("DEBUG-EXCEPTION-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Exception.class).includeBootstrap()
                .onBehavior("<init>")
                .onWatch(new AdviceListener() {

                    @Override
                    public void afterReturning(Advice advice) {
                        exLogger.info("occur an exception: {}",
                                advice.getTarget().getClass().getName(),
                                advice.getTarget()
                        );
                    }

                });
    }

}
