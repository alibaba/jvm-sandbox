package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatcher;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.textui.TTree;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 模仿Greys的trace命令
 * <p>测试用模块</p>
 */
@MetaInfServices(Module.class)
@Information(id = "debug-trace", version = "0.0.1", author = "luanjia@taobao.com")
public class DebugTraceModule extends HttpSupported implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Http("/trace")
    public void trace(final HttpServletRequest req,
                      final HttpServletResponse resp) throws IOException {
        try {
            final String cnPattern = getParameter(req, "class");
            final String mnPattern = getParameter(req, "method");
            final Printer printer = new ConcurrentLinkedQueuePrinter(resp.getWriter());
            debugTrace(cnPattern, mnPattern, printer);
        } catch (HttpSupported.HttpErrorCodeException hece) {
            resp.sendError(hece.getCode(), hece.getMessage());
            return;
        }
    }

    private void debugTrace(final String cnPattern,
                            final String mnPattern,
                            final Printer printer) {

        final EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
                .onClass(cnPattern).includeSubClasses()
                .onBehavior(mnPattern)
                .onWatching().withProgress(new ProgressPrinter(printer)).withCall()
                .onWatch(new AdviceListener() {

                    private String getTracingTitle(final Advice advice) {
                        return "Tracing for : "
                                + advice.getBehavior().getDeclaringClass().getName()
                                + "."
                                + advice.getBehavior().getName()
                                + " by "
                                + Thread.currentThread().getName()
                                ;
                    }

                    private String getEnterTitle(final Advice advice) {
                        return "Enter : "
                                + advice.getBehavior().getDeclaringClass().getName()
                                + "."
                                + advice.getBehavior().getName()
                                + "(...);"
                                ;
                    }

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        final TTree tTree;
                        if (advice.isProcessTop()) {
                            advice.attach(tTree = new TTree(true, getTracingTitle(advice)));
                        } else {
                            tTree = advice.getProcessTop().attachment();
                        }
                        tTree.begin(getEnterTitle(advice));
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        final TTree tTree = advice.getProcessTop().attachment();
                        tTree.end();
                        finish(advice);
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        final TTree tTree = advice.getProcessTop().attachment();
                        tTree.begin("throw:" + advice.getThrowable().getClass().getName() + "()").end();
                        tTree.end();
                        finish(advice);
                    }

                    private void finish(Advice advice) {
                        if (advice.isProcessTop()) {
                            final TTree tTree = advice.attachment();
                            printer.println(tTree.rendering());
                        }
                    }

                    @Override
                    protected void beforeCall(final Advice advice,
                                              final int callLineNum,
                                              final String callJavaClassName,
                                              final String callJavaMethodName,
                                              final String callJavaMethodDesc) {
                        final TTree tTree = advice.getProcessTop().attachment();
                        tTree.begin(callJavaClassName + ":" + callJavaMethodName + "(@" + callLineNum + ")");
                    }

                    @Override
                    protected void afterCallReturning(final Advice advice,
                                                      final int callLineNum,
                                                      final String callJavaClassName,
                                                      final String callJavaMethodName,
                                                      final String callJavaMethodDesc) {
                        final TTree tTree = advice.getProcessTop().attachment();
                        tTree.end();
                    }

                    @Override
                    protected void afterCallThrowing(final Advice advice,
                                                     final int callLineNum,
                                                     final String callJavaClassName,
                                                     final String callJavaMethodName,
                                                     final String callJavaMethodDesc,
                                                     final String callThrowJavaClassName) {
                        final TTree tTree = advice.getProcessTop().attachment();
                        tTree.set(tTree.get() + "[throw " + callThrowJavaClassName + "]").end();
                    }

                });

        try {
            printer.println(String.format(
                    "tracing on [%s#%s].\nPress CTRL_C abort it!",
                    cnPattern,
                    mnPattern
            ));
            printer.waitingForBroken();
        } finally {
            watcher.onUnWatched();
        }

    }

}
