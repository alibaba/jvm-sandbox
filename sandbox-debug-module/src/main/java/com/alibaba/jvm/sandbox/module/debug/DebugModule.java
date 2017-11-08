package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.CallBeforeEvent;
import com.alibaba.jvm.sandbox.api.event.CallReturnEvent;
import com.alibaba.jvm.sandbox.api.event.CallThrowsEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.asm.GaTraceClassVisitor;
import com.alibaba.jvm.sandbox.module.debug.util.GaEnumUtils;
import com.alibaba.jvm.sandbox.util.SandboxStringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * 测试用模块
 *
 * @author luanjia@taobao.com
 */
@Information(id = "debug", version = "0.0.0.5", author = "luanjia@taobao.com")
public class DebugModule implements Module {

    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Http("/asm")
    public void asm(final HttpServletRequest req,
                    final HttpServletResponse resp) throws IOException {

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

        final boolean isIncludeField = BooleanUtils.toBoolean(req.getParameter("field"));

        final Filter filter = new NamePatternFilter(classNamePattern, methodNamePattern);

        for (Class<?> clazz : loadedClassDataSource.find(filter)) {

            final InputStream is = clazz.getResourceAsStream("/" + SandboxStringUtils.toInternalClassName(clazz.getName()).concat(".class"));

            // 类没有找到(似乎不大可能哈)
            if (null == is) {
                continue;
            }

            // 不是所有类都能看字节码的
            if (clazz.isArray()) {
                continue;
            }

            final StringWriter stringWriter = new StringWriter();
            try {

                final ClassLoader loader = clazz.getClassLoader();

                final String title = String.format("// ASM BYTECODE FOR \"%s\" @ClassLoader:%s",
                        clazz.getName(),
                        loader
                );

                final ClassReader cr = new ClassReader(is);
                final GaTraceClassVisitor trace = new GaTraceClassVisitor(new PrintWriter(stringWriter, true)) {

                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                        if (isIncludeField) {
                            return super.visitField(access, name, desc, signature, value);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        if (SandboxStringUtils.matching(name, methodNamePattern)) {
                            return super.visitMethod(access, name, desc, signature, exceptions);
                        } else {
                            return null;
                        }
                    }

                };
                cr.accept(trace, ClassReader.SKIP_DEBUG);

                resp.getWriter().println("\n");
                resp.getWriter().println(title);
                resp.getWriter().println(stringWriter.toString());

            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(stringWriter);
            }

        }
    }


    enum Trigger {
        BEFORE,
        RETURN,
        THROWS
    }


    // -d 'debug/watch?class=com.alibaba.*&method=newAddress&trigger=RETURN&watch=return&expand=1&progress=true'
    @Http("/watch")
    public void watch(final HttpServletRequest req,
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

        final String watchExpress = req.getParameter("watch");
        if (StringUtils.isBlank(watchExpress)) {
            resp.sendError(SC_BAD_REQUEST, "watch parameter was required.");
            return;
        }

        final Set<Trigger> triggers = GaEnumUtils.valuesOf(
                Trigger.class,
                req.getParameterValues("trigger"),
                new Trigger[]{Trigger.BEFORE}
        );

        int expand;
        try {
            expand = Integer.getInteger(req.getParameter("expand"));
        } catch (Throwable cause) {
            expand = 1;
        }

        final Printer printer = new ConcurrentLinkedQueuePrinter(resp.getWriter(), 20, 20, 2000);

        final ModuleEventWatcher.Progress progress =
                Boolean.valueOf(req.getParameter("progress"))
                        ? new ProgressPrinter("PROGRESS", 20, printer)
                        : null;

        try {

            moduleEventWatcher.watching(
                    new NamePatternFilter(classNamePattern, methodNamePattern),
                    new WatchEventListener(printer, triggers, watchExpress, expand),
                    progress,
                    new ModuleEventWatcher.WatchCallback() {
                        @Override
                        public void watchCompleted() throws Throwable {
                            printer.waitingForBroken();
                        }
                    },
                    null,
                    RETURN, BEFORE, THROWS
            );

        } finally {
            printer.close();
        }

    }


    private Event.Type[] toCallEventTypeArray(final Set<Trigger> triggers) {
        final Set<Event.Type> events = new LinkedHashSet<Event.Type>();
        for (Trigger trigger : triggers) {
            switch (trigger) {
                case BEFORE:
                    events.add(Event.Type.CALL_BEFORE);
                    break;
                case RETURN:
                    events.add(Event.Type.CALL_RETURN);
                    break;
                case THROWS:
                    events.add(Event.Type.CALL_THROWS);
                    break;
            }
        }
        return events.toArray(new Event.Type[]{});
    }


    // -d 'debug/tracing?class=com.alibaba.*&method=newAddress'
    @Http("/tracing")
    public void tracing(final HttpServletRequest req,
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

        final Set<Trigger> triggers = GaEnumUtils.valuesOf(
                Trigger.class,
                req.getParameterValues("trigger"),
                new Trigger[]{Trigger.BEFORE}
        );

        final Printer printer = new ConcurrentLinkedQueuePrinter(resp.getWriter(), 20, 20, 2000);

        try {

            moduleEventWatcher.watching(
                    new NamePatternFilter(classNamePattern, methodNamePattern),
                    new EventListener() {
                        @Override
                        public void onEvent(final Event event) throws Throwable {
                            switch (event.type) {
                                case CALL_BEFORE:
                                    final CallBeforeEvent tbEvent = (CallBeforeEvent) event;
                                    printer.println(String.format("CALL_BEFORE:processId=%d;invokeId=%d;lineNumber=%d;owner=%s;name=%s;desc=%s;",
                                            tbEvent.processId, tbEvent.invokeId, tbEvent.lineNumber, tbEvent.owner, tbEvent.name, tbEvent.desc));
                                    break;
                                case CALL_RETURN:
                                    final CallReturnEvent trEvent = (CallReturnEvent) event;
                                    printer.println(String.format("CALL_RETURN:processId=%d;invokeId=%d;",
                                            trEvent.processId, trEvent.invokeId));
                                    break;
                                case CALL_THROWS:
                                    final CallThrowsEvent ttEvent = (CallThrowsEvent) event;
                                    printer.println(String.format("CALL_THROWS:processId=%d;invokeId=%d;exception=%s;",
                                            ttEvent.processId, ttEvent.invokeId, ttEvent.throwException));
                                    break;
                                default:
                                    printer.println("unExpect event=" + event);
                            }
                        }
                    },
                    new ModuleEventWatcher.WatchCallback() {
                        @Override
                        public void watchCompleted() throws Throwable {
                            printer.waitingForBroken();
                        }
                    },
                    toCallEventTypeArray(triggers)
            );
        } finally {
            printer.close();
        }

    }


    @Http("/broken")
    public void broken(final HttpServletRequest req,
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

        final Printer printer = new ConcurrentLinkedQueuePrinter(resp.getWriter(), 20, 20, 2000);

        try {

            moduleEventWatcher.watching(
                    new NamePatternFilter(classNamePattern, methodNamePattern),
                    new EventListener() {
                        @Override
                        public void onEvent(final Event event) throws Throwable {
                            printer.println(String.format("RECEIVE EVENT=%s;", event));
                            ProcessControlException.throwThrowsImmediately(new RuntimeException("TEST FOR JVM-SANDBOX!"));
                        }
                    },
                    new ModuleEventWatcher.WatchCallback() {
                        @Override
                        public void watchCompleted() throws Throwable {
                            printer.waitingForBroken();
                        }
                    },
                    Event.Type.RETURN, Event.Type.THROWS
            );
        } finally {
            printer.close();
        }

    }


}
