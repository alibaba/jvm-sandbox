package com.alibaba.jvm.sandbox.core.manager.async;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;

/**
 * Quintuple for event watching probe
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/11 4:34 下午
 */
public class TransformationQuintuple {

    private int watchId;
    private Matcher matcher;
    private EventListener listener;
    private ModuleEventWatcher.Progress progress;
    private Event.Type[] eventTypes;

    public int getWatchId() {
        return watchId;
    }

    public void setWatchId(int watchId) {
        this.watchId = watchId;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    public EventListener getListener() {
        return listener;
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    public ModuleEventWatcher.Progress getProgress() {
        return progress;
    }

    public void setProgress(ModuleEventWatcher.Progress progress) {
        this.progress = progress;
    }

    public Event.Type[] getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(Event.Type[] eventTypes) {
        this.eventTypes = eventTypes;
    }
}
