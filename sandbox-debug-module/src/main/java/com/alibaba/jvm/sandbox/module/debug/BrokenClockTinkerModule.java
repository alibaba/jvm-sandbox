package com.alibaba.jvm.sandbox.module.debug;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

@MetaInfServices(Module.class)
@Information(id = "broken-clock-tinker")
public class BrokenClockTinkerModule implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;


    private long baseMockTime = 1631750400000L;

    private volatile long baseTime = 0;

    @Command("repairCheckState")
    public void repairCheckState() {
        baseTime = System.currentTimeMillis();
        new EventWatchBuilder(moduleEventWatcher)
            .onClass("java.lang.System").includeBootstrap()
            .onBehavior("currentTimeMillis")
            .onWatch(new AdviceListener() {

                @Override
                protected void before(Advice advice) throws Throwable {
                }

                @Override
                protected void after(Advice advice) throws Throwable {
                    long time = (Long)advice.getReturnObj();
                    ProcessController.returnImmediately(baseMockTime + (time - baseTime));
                }
            });
    }

}