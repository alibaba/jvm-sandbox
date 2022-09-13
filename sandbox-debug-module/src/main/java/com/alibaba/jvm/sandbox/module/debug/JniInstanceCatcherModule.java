package com.alibaba.jvm.sandbox.module.debug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.alibaba.jvm.sandbox.api.resource.JniAnchorManager;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.alibaba.jvm.sandbox.module.debug.util.Express;
import com.alibaba.jvm.sandbox.module.debug.util.Express.ExpressFactory;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

/**
 * {@link JniInstanceCatcherModule}
 * <p>
 * 基于{@link com.alibaba.jvm.sandbox.api.resource.JniAnchorManager }
 *
 * @author zhaoyb1990
 */
@MetaInfServices(Module.class)
@Information(id = "debug-jni", version = "0.0.1", author = "zhaoyb1990@foxmail.com")
public class JniInstanceCatcherModule extends ParamSupported implements Module {

    @Resource
    private JniAnchorManager jniAnchorManager;
    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    @Command("getInstances")
    public void getInstances(final Map<String, String> param,
            final Map<String, String[]> params,
            final PrintWriter writer) {

        final String className = getParameter(param, "className");
        final String express = getParameter(param, "express");
        final Integer limit = getParameter(param, "limit", Integer.class, 100);
        final Printer printer = new ConcurrentLinkedQueuePrinter(writer);

        try {
            checkArgs("className", className);
            checkArgs("express", express);
        } catch (IllegalArgumentException e) {
            printer.println(e.getMessage());
            flushAndExit(printer);
            return;
        }

        final Stopwatch stopwatch = Stopwatch.createStarted();
        // try to find class with loadedClassDataSource;
        List<Class<?>> targetClasses = findTargetClasses(className);
        printer.println(String.format("FindMatchingClass>>::classCount=%s,cost=%sms", targetClasses.size(),
                stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
        if (targetClasses.size() == 0) {
            printer.println(String.format("Class Not Found>>::className=%s", className));
            flushAndExit(printer);
            return;
        }
        // try to find class instance and express;
        for (Class<?> targetClass : targetClasses) {
            tryGetInstance(targetClass, printer, express, limit);
        }
        flushAndExit(printer);
    }

    private void flushAndExit(final Printer printer) {
        printer.flush();
        printer.close();
    }

    private void tryGetInstance(final Class<?> targetClass, final Printer printer,
            final String express, final int limit) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        Object[] instances = jniAnchorManager.getInstances(targetClass, limit);
        printer.println(String.format("FindTargetInstance>>::className=%s,classloader=%s,cost=%s{ms}",
                targetClass.getName(), targetClass.getClassLoader(), stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
        if (instances != null && instances.length > 0) {
            for (Object instance : instances) {
                tryEvalExpress(instance, printer, express);
            }
        }
    }

    private void tryEvalExpress(final Object instance, final Printer printer, final String express) {
        try {
            Object evalValue = ExpressFactory.newExpress(instance).get(express);
            printer.println(String.format("Instance eval finish>>:: className=%s,classloader=%s,evalValue=%s",
                    instance.getClass().getName(), instance.getClass().getClassLoader(), toString(evalValue)));
        } catch (Express.ExpressException e) {
            printer.println(String.format("Instance eval failed>>:: express=%s ; msg:%s.", express, e.getMessage()));
        }
    }

    private String toString(final Object object) {
        return null == object ? "null" : object.toString();
    }

    private List<Class<?>> findTargetClasses(String className) {
        final List<Class<?>> matchedClass = new ArrayList<Class<?>>();
        Iterator<Class<?>> classIterator = loadedClassDataSource.iteratorForLoadedClasses();
        while (classIterator.hasNext()) {
            Class<?> clazz = classIterator.next();
            // getCanonicalName not works well in some cases;
            if (className.equals(clazz.getName())) {
                matchedClass.add(clazz);
            }
        }
        return matchedClass;
    }

    private void checkArgs(String argName, String argValue) throws IllegalArgumentException {
        if (StringUtils.isEmpty(argValue)) {
            throw new IllegalArgumentException(String.format("illegal argument, %s cannot be null or empty.", argName));
        }
    }

}
