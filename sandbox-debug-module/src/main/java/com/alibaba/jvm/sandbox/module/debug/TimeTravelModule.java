package com.alibaba.jvm.sandbox.module.debug;

import javax.annotation.Resource;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;

import org.kohsuke.MetaInfServices;

/**
 * 时间修改例子
 *
 * 如果增强的方法是/hotspot/src/share/vm/classfile/vmSymbols.hpp 宏定义：
 * #define VM_INTRINSICS_DO(do_intrinsic, do_class, do_name, do_signature, do_alias)
 * 所定义的native方法,并且实际运行中发现增强过一段时间会失效,则需要VM参数中添加:
 * -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=dontinline,${ClassName}::${MethodName} -XX:DisableIntrinsic=${MethodNameId}
 *
 * ${ClassName}: 类全限定名
 * {MethodName}: 类方法名
 * ${MethodNameId}：intrinsic id，  从宏定义：VM_INTRINSICS_DO第一个参数获取
 *
 * 举例java.lang.System.currentTimeMillis
 * -XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=dontinline,java.lang.System::currentTimeMillis -XX:DisableIntrinsic=_currentTimeMillis
 *
 */
@MetaInfServices(Module.class)
@Information(id = "time-travel", version = "1.0.0" , author = "zhuangpeng.zp@alibaba-inc.com")
public class TimeTravelModule implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    /**
     * 时间穿越的起点, 2019/10/10 0:0:0
     */
    private long baseMockTime = 1570636800000L;

    /**
     * baseTime 为穿越一刻的时间,举例2020/11/11 0:0:0 发起穿越,baseTime即为这一刻的Unix时间戳
     */
    private volatile long baseTime = 0;

    @Command("travel")
    public void travel() {
        //初始化baseTime
        baseTime = System.currentTimeMillis();
        new EventWatchBuilder(moduleEventWatcher)
            .onClass("java.lang.System")
            .includeBootstrap()
            .onBehavior("currentTimeMillis")
            .onWatch(new AdviceListener() {

                @Override
                protected void before(Advice advice) throws Throwable {
                }

                @Override
                protected void afterReturning(Advice advice) throws Throwable {
                    //time是真实的时间
                    long time = (Long)advice.getReturnObj();
                    //根据真实时间到baseTime的差值，来模拟过去时间的流逝
                    ProcessController.returnImmediately(baseMockTime + (time - baseTime));
                }
            });
    }
}