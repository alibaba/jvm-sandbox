package com.alibaba.jvm.sandbox.module.junit.probe;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycleAdapter;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.AccessFlags;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

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

        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.WILDCARD)
                .onClass("javax.servlet.http.HttpServlet")
                .includeBootstrap()
                .includeSubClasses()
                .withAccess(AccessFlags.ACF_PUBLIC)
                .onBehavior("service")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )
                .onWatch(new ServletListner(), Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS);

        new EventWatchBuilder(moduleEventWatcher, EventWatchBuilder.PatternType.WILDCARD)
                .onClass(String.class)
                .includeBootstrap()
                .includeSubClasses()
                .withAccess(AccessFlags.ACF_PUBLIC)
                .onBehavior("<init>")
                .withParameterTypes(String.class)
                .onWatch(new StringListener(), Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS);


    }


}
