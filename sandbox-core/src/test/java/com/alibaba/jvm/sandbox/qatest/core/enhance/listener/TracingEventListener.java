package com.alibaba.jvm.sandbox.qatest.core.enhance.listener;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.junit.Assert.assertEquals;

/**
 * 事件跟踪监听器
 */
public class TracingEventListener extends InterruptedEventListener {

    private final List<Event.Type> eventTracing = new ArrayList<Event.Type>();

    @Override
    public void onEvent(Event event) throws Throwable {
        eventTracing.add(event.type);
    }

    /**
     * 获取跟踪信息
     *
     * @return 跟踪信息
     */
    public List<Event.Type> getEventTracing() {
        return eventTracing;
    }

    /**
     * 断言跟踪信息
     *
     * @param exceptEventTypes 期待的事件类型
     */
    public void assertEventTracing(final Event.Type... exceptEventTypes) {
        assertEventProcessor();
        assertArrayEquals(
                exceptEventTypes,
                getEventTracing().toArray(new Event.Type[]{})
        );
    }

    // 检查内核事件处理器是否正确
    private void assertEventProcessor() {
        EventListenerHandlers
                .getSingleton()
                .checkEventProcessor(ObjectIDs.instance.identity(this));
    }

    private <E> void assertArrayEquals(E[] exceptArray, E[] actualArray) {
        assertEquals("except size not matched!", getLength(exceptArray), getLength(actualArray));
        for (int index = 0; index < exceptArray.length; index++) {
            assertEquals("[" + index + "] not matched", exceptArray[index], actualArray[index]);
        }
    }


}
