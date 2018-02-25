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
 * Spring容器的调试日志
 */
@MetaInfServices(Module.class)
@Information(id = "debug-spring-logger", version = "0.0.1", author = "luanjia@taobao.com")
public class SpringLoggerModule implements Module, LoadCompleted {

    private final Logger spLogger = LoggerFactory.getLogger("DEBUG-SPRING-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        buildingSpringRestController();
    }

    private void buildingSpringRestController() {
        new EventWatchBuilder(moduleEventWatcher)
                .onAnyClass()
                .hasAnnotationTypes("org.springframework.web.bind.annotation.RestController")
                .onAnyBehavior()
                .hasAnnotationTypes("org.springframework.web.bind.annotation.RequestMapping")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) {
                        advice.attach(System.currentTimeMillis());
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        logSpringRestController(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        logSpringRestController(advice);
                    }

                    private void logSpringRestController(Advice advice) {
                        logSpring(
                                "REST",
                                System.currentTimeMillis() - (Long) advice.attachment(),
                                advice.getTarget().getClass().getName() + "#" + advice.getBehavior().getName(),
                                advice.isReturn() ? "SUC" : "FAL",
                                advice.isThrows() ? advice.getThrowable() : null
                        );
                    }

                });
    }

    /*
     * 时间：日志框架提供
     * 地点：SpringMod
     * 耗时：costMs
     * 人物：who
     * 事件：message/throwable
     */
    private void logSpring(final String springMod,
                           final long cost,
                           final String who,
                           final String message,
                           final Throwable throwable) {
        spLogger.info("{}:{}ms:{}:{}", springMod, cost, who, message, throwable);
    }

}
