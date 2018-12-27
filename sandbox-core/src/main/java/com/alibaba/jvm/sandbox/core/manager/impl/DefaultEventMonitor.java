package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.resource.EventMonitor;

/**
 * 事件监控器实现
 */
class DefaultEventMonitor implements EventMonitor {

    @Override
    public EventPoolInfo getEventPoolInfo() {
        return new EventPoolInfo() {
            @Override
            public int getNumActive() {
                return 0;
            }

            @Override
            public int getNumActive(Event.Type type) {
                return 0;
            }

            @Override
            public int getNumIdle() {
                return 0;
            }

            @Override
            public int getNumIdle(Event.Type type) {
                return 0;
            }
        };
    }

}
