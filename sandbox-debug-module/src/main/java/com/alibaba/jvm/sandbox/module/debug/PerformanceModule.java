package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.util.GaEnumUtils;
import com.alibaba.jvm.sandbox.module.debug.util.SimpleDateFormatHolder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * 用于性能测试用的模块
 * <p>
 * 该模块将会对指定的方法插入对应的事件，所有的事件为空流转，即不作处理
 * </p>
 *
 * @author luanjia@taobao.com
 */
@Information(id = "perf", author = "luanjia@taobao.com", version = "0.0.0.1")
public class PerformanceModule implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    /**
     * 类名匹配：{@code class=<class name pattern>}<br>
     * 方法名匹配：{@code method=<method name pattern>}<br>
     * 是否输出性能指标：{@code print=true}<br>
     *
     * @param req  HttpServletRequest
     * @param resp HttpServletResponse
     * @throws Throwable handle occur error
     */
    @Http("/perf")
    public void perf(final HttpServletRequest req,
                     final HttpServletResponse resp) throws Throwable {


        final String classNamePattern = req.getParameter("class");
        if (StringUtils.isBlank(classNamePattern)) {
            resp.sendError(SC_BAD_REQUEST, "class parameter was required.");
            return;
        }

        final String methodNamePattern = req.getParameter("method");
        if (StringUtils.isBlank(methodNamePattern)) {
            resp.sendError(SC_BAD_REQUEST, "method parameter was required.");
            return;
        }

        final Set<Event.Type> events = GaEnumUtils.valuesOf(
                Event.Type.class,
                req.getParameterValues("event"),
                Event.Type.values()
        );

        final boolean isPrint = BooleanUtils.toBoolean(req.getParameter("print"));

        final Printer printer = new ConcurrentLinkedQueuePrinter(resp.getWriter());

        final AtomicBoolean isRunningRef = new AtomicBoolean(isPrint);

        try {
            moduleEventWatcher.watching(
                    new NamePatternFilter(classNamePattern, methodNamePattern),
                    new PerformanceEventListener(isPrint, isRunningRef, printer),
                    new ProgressPrinter(">", 10, printer),
                    new ModuleEventWatcher.WatchCallback() {
                        @Override
                        public void watchCompleted() throws Throwable {
                            printer.waitingForBroken();
                            isRunningRef.set(false);
                        }
                    },
                    new ProgressPrinter(">", 10, printer),
                    events.toArray(new Event.Type[]{})
            );
        } finally {
            printer.close();
        }


    }


    /**
     * 用于性能分析用的
     */
    class PerformanceEventListener implements EventListener {

        static final long INTERVAL_MS = 1000 * 60;

        final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        volatile ConcurrentHashMap<Event.Type, AtomicInteger> eventCountMap = new ConcurrentHashMap<Event.Type, AtomicInteger>();


        final Printer printer;
        final boolean isPrint;
        final AtomicBoolean isRunningRef;
        final Thread tPrinter = new Thread("perf-module-printer") {

            final DecimalFormat df = new DecimalFormat(".##");

            @Override
            public void run() {

                printer.println("perf output was running, interval=" + INTERVAL_MS + "ms...");

                try {
                    while (isRunningRef.get()
                            && !Thread.currentThread().isInterrupted()) {

                        // 休眠xxMs后开始统计
                        Thread.sleep(INTERVAL_MS);

                        // 切换统计数据
                        final Map<Event.Type, AtomicInteger> _eventCountMap = eventCountMap;
                        rwLock.writeLock().lock();
                        try {
                            eventCountMap = new ConcurrentHashMap<Event.Type, AtomicInteger>();
                        } finally {
                            rwLock.writeLock().unlock();
                        }

                        // 进行统计
                        final StringBuilder outputSB = new StringBuilder(SimpleDateFormatHolder.getInstance().format(new Date()))
                                .append("\n");
                        int total = 0;
                        for (Map.Entry<Event.Type, AtomicInteger> entry : _eventCountMap.entrySet()) {
                            final int eventCount = entry.getValue().get();
                            total += eventCount;
                            outputSB
                                    .append("    ")
                                    .append(entry.getKey())
                                    .append(":")
                                    .append(eventCount)
                                    .append("\n")
                            ;
                        }
                        outputSB
                                .append("TOTAL:").append(total).append("\n")
                                .append("RATE:").append(df.format(total/(INTERVAL_MS/1000))).append("/sec\n");


                        // 统计输出
                        printer.println(outputSB.toString());

                    }
                } catch (InterruptedException e) {
                    //
                }
            }

        };

        PerformanceEventListener(final boolean isPrint,
                                 final AtomicBoolean isRunningRef,
                                 final Printer printer) {
            this.isPrint = isPrint;
            this.isRunningRef = isRunningRef;
            this.printer = printer;

            if (isPrint) {
                tPrinter.setDaemon(true);
                tPrinter.start();
            }

        }

        @Override
        public void onEvent(Event event) throws Throwable {

            if (!isPrint) {
                return;
            }

            rwLock.readLock().lock();
            try {

                AtomicInteger countRef = eventCountMap.get(event.type);
                if(null == countRef) {
                    countRef = new AtomicInteger(0);
                    final AtomicInteger tempCountRef = eventCountMap.putIfAbsent(event.type, countRef);
                    if(null != tempCountRef) {
                        countRef = tempCountRef;
                    }
                }

                countRef.incrementAndGet();

            } finally {
                rwLock.readLock().unlock();
            }

        }

    }


}
