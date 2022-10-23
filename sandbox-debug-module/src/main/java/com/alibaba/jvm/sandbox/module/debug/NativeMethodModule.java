package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

/**
 *
 * native方法增强
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
 * @author zhuangpeng
 * @since 2020/11/27
 */
@MetaInfServices(Module.class)
@Information(id = "native", version = "1.0.0" , author = "zhuangpeng.zp@alibaba-inc.com")
public class NativeMethodModule implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;


    @Command("class")
    public void travel() {
        //初始化baseTime
        new EventWatchBuilder(moduleEventWatcher)
            .onClass("java.lang.Class")
            .includeBootstrap()
            .onBehavior("isInterface")
            .onWatch(new AdviceListener() {

                @Override
                protected void before(Advice advice) throws Throwable {
                    System.out.println("isInterface method enter");
                }

                @Override
                protected void afterReturning(Advice advice) throws Throwable {
                    System.out.println("isInterface method out : " + advice.getReturnObj());
                }

                @Override
                protected void afterThrowing(Advice advice) throws Throwable {
                    System.out.println("isInterface method ouccer exception" + advice.getReturnObj());
                }
            });
    }
}