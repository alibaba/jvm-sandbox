package com.alibaba.jvm.sandbox.module.debug;

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

    @Command("repairCheckState")
    public void repairCheckState() {

        new EventWatchBuilder(moduleEventWatcher)
            .onClass("java.lang.System").includeBootstrap()
            .onBehavior("currentTimeMillis")
            .onWatch(new AdviceListener() {

                /**
                 * 拦截{@code com.taobao.demo.Clock#checkState()}方法，当这个方法抛出异常时将会被
                 * AdviceListener#afterThrowing()所拦截
                 */
                @Override
                protected void afterThrowing(Advice advice) throws Throwable {

                    // 在此，你可以通过ProcessController来改变原有方法的执行流程
                    // 这里的代码意义是：改变原方法抛出异常的行为，变更为立即返回；void返回值用null表示
                    ProcessController.returnImmediately(null);
                }
            });

    }

}