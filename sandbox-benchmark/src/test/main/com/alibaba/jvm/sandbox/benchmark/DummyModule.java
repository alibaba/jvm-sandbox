package com.alibaba.jvm.sandbox.benchmark;

import ch.qos.logback.core.util.TimeUtil;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycleAdapter;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.AccessFlags;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.listener.ext.WatchIdHolder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.api.util.SandboxProtector;
import com.alibaba.jvm.sandbox.api.util.StopAllSandboxProtector;
import com.google.common.base.Stopwatch;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 2:54 下午
 */
@MetaInfServices(Module.class)
@Information(id = "dummy-module", version = "1.0.0", author = "renyi.cry")
public class DummyModule extends ModuleLifecycleAdapter implements Module {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {

        Stopwatch stopwatch = Stopwatch.createStarted();

        SandboxProtector currentProtector = SandboxProtector.SandboxProtectors.getInstance();
        SandboxProtector stopAllSandboxProtector = new StopAllSandboxProtector(currentProtector);

        SandboxProtector.SandboxProtectors.force2resetInstance(stopAllSandboxProtector);
        try {

            Class[] testClasses = new Class[20];
            Arrays.fill(testClasses, TestClass.class);

            for (int i = 0; i < 9; i++) {

                watch(Character.toString((char)('a' + i)), testClasses);

            }

            while (Holder.withLazyReload && WatchIdHolder.hasUnfinishedWatchId()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            while (WatchIdHolder.hasUnfinishedWatchId()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SandboxProtector.SandboxProtectors.force2resetInstance(currentProtector);
        }


        if (Holder.withLazyReload) {
            System.out.println("parallel time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } else {
            System.out.println("single time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }

    }

    private void watch(String method, Class... classes) {

        for (Class clazz : classes) {

            new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.WILDCARD)
                    .onClass(clazz)
                    .includeBootstrap()
                    .includeSubClasses()
                    .withAccess(AccessFlags.ACF_PUBLIC)
                    .onBehavior(method)
                    .withLazyReload(Holder.withLazyReload)
                    .onWatch(new DummyListener(), Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS);

        }

    }


}
