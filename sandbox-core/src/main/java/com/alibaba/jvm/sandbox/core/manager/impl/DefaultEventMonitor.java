package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.resource.EventMonitor;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.util.EventPool;

/**
 * 事件监控器实现
 */
class DefaultEventMonitor implements EventMonitor {

    @Override
    public EventPoolInfo getEventPoolInfo() {

        final EventPool pool = EventListenerHandlers.getSingleton().getEventPool();

        return new EventPoolInfo() {
            @Override
            public int getNumActive() {
                return pool.getNumActive();
            }

            @Override
            public int getNumActive(Event.Type type) {
                return pool.getNumActive(type);
            }

            @Override
            public int getNumIdle() {
                return pool.getNumIdle();
            }

            @Override
            public int getNumIdle(Event.Type type) {
                return pool.getNumIdle(type);
            }
        };
    }

}
