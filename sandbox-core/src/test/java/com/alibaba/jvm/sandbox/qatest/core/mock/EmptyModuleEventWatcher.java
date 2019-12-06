package com.alibaba.jvm.sandbox.qatest.core.mock;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;

public class EmptyModuleEventWatcher implements ModuleEventWatcher {

    @Override
    public int watch(Filter filter, EventListener listener, Progress progress, Event.Type... eventType) {
        return 0;
    }

    @Override
    public int watch(Filter filter, EventListener listener, Event.Type... eventType) {
        return 0;
    }

    @Override
    public int watch(EventWatchCondition condition, EventListener listener, Progress progress, Event.Type... eventType) {
        return 0;
    }

    @Override
    public void delete(int watcherId, Progress progress) {

    }

    @Override
    public void delete(int watcherId) {

    }

    @Override
    public void watching(Filter filter, EventListener listener, Progress wProgress, WatchCallback watchCb, Progress dProgress, Event.Type... eventType) throws Throwable {

    }

    @Override
    public void watching(Filter filter, EventListener listener, WatchCallback watchCb, Event.Type... eventType) throws Throwable {

    }
}
