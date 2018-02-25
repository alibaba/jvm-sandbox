package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.Sentry;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.alibaba.jvm.sandbox.module.debug.HttpHttpAccessLoggerModule.HttpProcessStep.*;

/**
 * 基于HTTP-SERVLET(v2.4)规范的HTTP访问日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "debug-http-logger", version = "0.0.1", author = "luanjia@taobao.com")
public class HttpHttpAccessLoggerModule implements Module, LoadCompleted {

    private final Logger stLogger = LoggerFactory.getLogger("DEBUG-SERVLET-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;


    /**
     * HTTP处理步骤
     * {@code
     * HttpServlet.service():BEGIN
     * -> HttpServletResponse.[setState/sendError]():BEGIN
     * -> HttpServletResponse.[setState/sendError]():FINISH
     * -> HttpServlet.service():FINISH
     * }
     */
    enum HttpProcessStep {
        waitingHttpServletServiceBegin,
        waitingHttpServletResponseState,
        waitingHttpServletServiceFinish
    }

    /**
     * HTTP接入信息
     */
    class HttpAccess {
        String from;
        String method;
        String uri;
        Map<String, String[]> parameterMap;
        String userAgent;
        int status;
        long beginTimestamp;
    }

    // 安排一个哨兵，用于观察Servlet执行步骤
    private final Sentry<HttpProcessStep> sentry = new Sentry<HttpProcessStep>(waitingHttpServletServiceBegin);

    @Override
    public void loadCompleted() {
        buildingHttpStatusFillBack();
        buildingHttpServletService();
    }

    /*
     * HTTP状态码回填
     * 因为在3.0之前你都很难拿到HTTP的应答状态，必须拦截HttpServletResponse的setStatus/sendError才能拿到
     * 而且还必须要考虑到200这种状态码为默认状态码的情况
     */
    private void buildingHttpStatusFillBack() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("javax.servlet.http.HttpServletResponse")
                /**/.includeSubClasses()
                .onBehavior("setStatus")
                /**/.withParameterTypes(int.class)
                .onBehavior("sendError")
                /**/.withParameterTypes(int.class)
                /**/.withParameterTypes(int.class, String.class)
                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) {
                        if (sentry.next(waitingHttpServletResponseState, waitingHttpServletServiceFinish)) {
                            final HttpAccess ha = sentry.attachment();
                            ha.status = (Integer) advice.getParameterArray()[0];
                        }
                    }
                });
    }

    /*
     * 拦截HttpServlet的服务请求入口
     */
    private void buildingHttpServletService() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("javax.servlet.http.HttpServlet")
                .includeBootstrap()
                .onBehavior("service")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )
                .onWatch(new AdviceListener() {

                    final String MARK_HTTP_BEGIN = "MARK_HTTP_BEGIN";

                    @Override
                    public void before(Advice advice) throws Throwable {
                        if (sentry.next(waitingHttpServletServiceBegin, waitingHttpServletResponseState)) {
                            final Object objectOfHttpServletRequest = advice.getParameterArray()[0];
                            final HttpAccess ha = new HttpAccess();
                            ha.from = invokeMethod(objectOfHttpServletRequest, "getRemoteAddr");
                            ha.method = invokeMethod(objectOfHttpServletRequest, "getMethod");
                            ha.uri = invokeMethod(objectOfHttpServletRequest, "getRequestURI");
                            ha.parameterMap = invokeMethod(objectOfHttpServletRequest, "getParameterMap");
                            ha.userAgent = invokeMethod(objectOfHttpServletRequest, "getHeader", "User-Agent");
                            ha.beginTimestamp = System.currentTimeMillis();
                            sentry.attach(ha);
                            advice.mark(MARK_HTTP_BEGIN);
                        }
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (finishing(advice)) {
                            final HttpAccess ha = sentry.attachment();
                            final long cost = System.currentTimeMillis() - ha.beginTimestamp;
                            logAccess(ha, cost, null);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        if (finishing(advice)) {
                            final HttpAccess ha = sentry.attachment();
                            final long cost = System.currentTimeMillis() - ha.beginTimestamp;
                            logAccess(ha, cost, advice.getThrowable());
                        }
                    }

                    /**
                     * 判断是否请求对称结束
                     *
                     * @param advice 通知
                     * @return TRUE:对称结束;FALSE:非对称结束
                     */
                    private boolean finishing(Advice advice) {

                        // 一些HttpServletResponse在实现的时候，status默认值就是200
                        // 所以这些实现类就会偷懒不去主动调用setStatus()，仅拦截setStatus/sendError是无法完全获取所有的，HTTP状态码
                        // 这里做一个判断，如果走的是默认值，则主动补偿上这个200
                        //
                        // 调用路径
                        // HttpServlet.service():BEGIN
                        //  `-HttpServlet.service():FINISH
                        if (advice.hasMark(MARK_HTTP_BEGIN)
                                && sentry.next(waitingHttpServletResponseState, waitingHttpServletServiceBegin)) {
                            final HttpAccess ha = sentry.attachment();
                            ha.status = 200;
                            return true;
                        }

                        // 调用路径
                        // HttpServlet.service():BEGIN
                        //  `- HttpServletResponse.[setState/sendError]():BEGIN
                        //      `- HttpServletResponse.[setState/sendError]():FINISH
                        //          `- HttpServlet.service():FINISH
                        return advice.hasMark(MARK_HTTP_BEGIN)
                                && sentry.next(waitingHttpServletServiceFinish, waitingHttpServletServiceBegin);

                    }

                });
    }

    /*
     * 泛型转换方法调用
     * 底层使用apache common实现
     */
    private static <T> T invokeMethod(final Object object,
                                      final String methodName,
                                      final Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return (T) MethodUtils.invokeMethod(object, methodName, args);
    }

    // 格式化ParameterMap
    private static String formatParameterMap(final Map<String, String[]> parameterMap) {
        final Set<String> kvPairs = new LinkedHashSet<String>();
        for (final Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            final String[] valueArray = entry.getValue();
            if (null == valueArray) {
                continue;
            }
            for (final String value : valueArray) {
                kvPairs.add(String.format("%s=%s", entry.getKey(), value));
            }
        }
        return StringUtils.join(kvPairs, "&");
    }


    /*
     * 记录access日志
     */
    private void logAccess(final HttpAccess ha,
                           final long costMs,
                           final Throwable cause) {
        stLogger.info("{};{};{};{}ms;{};[{}];{};",
                ha.from,
                ha.status,
                ha.method,
                costMs,
                ha.uri,
                formatParameterMap(ha.parameterMap),
                ha.userAgent,
                cause
        );
    }

}
