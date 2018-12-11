package com.alibaba.jvm.sandbox.qatest.core.enhance.listener;

import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.enhance.annotation.Interrupted;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * 事件流校验器
 */
@Interrupted
public class EventStreamCheckerListener implements EventListener {

    private final Queue<EventChecker> checkers = new LinkedList<EventChecker>();

    /**
     * 下一个事件校验
     *
     * @param checker 事件校验器
     * @return this
     */
    public EventStreamCheckerListener nextEventCheck(EventChecker<? extends Event> checker) {
        checkers.offer(checker);
        return this;
    }

    @Override
    public void onEvent(Event event) throws Throwable {
        checkers.poll().onCheck(event);
    }

    /**
     * 当事件流结束时，Check队列必须为空！
     */
    public void assertIsEmpty() {
        Assert.assertTrue("Event checker queue is not empty!", checkers.isEmpty());
    }


    /**
     * 事件校验器
     */
    public interface EventChecker<E extends Event> {

        /**
         * 校验事件是否正确
         *
         * @param event 事件
         */
        void onCheck(E event) throws Throwable;

    }

    /**
     * 事件类型校验器
     *
     * @param <E> 事件
     */
    public static final class EventTypeChecker<E extends Event> implements EventChecker {

        private final Event.Type type;

        public EventTypeChecker(Event.Type type) {
            this.type = type;
        }

        @Override
        public void onCheck(Event event) {
            Assert.assertThat("Event.Type was not match!", type, equalTo(event.type));
        }

        public static final EventTypeChecker<BeforeEvent> BEFORE_CHECKER = new EventTypeChecker<BeforeEvent>(Event.Type.BEFORE);
        public static final EventTypeChecker<ReturnEvent> RETURN_CHECKER = new EventTypeChecker<ReturnEvent>(Event.Type.RETURN);
        public static final EventTypeChecker<ThrowsEvent> THROWS_CHECKER = new EventTypeChecker<ThrowsEvent>(Event.Type.THROWS);
        public static final EventTypeChecker<CallBeforeEvent> CALL_BEFORE_CHECKER = new EventTypeChecker<CallBeforeEvent>(Event.Type.CALL_BEFORE);
        public static final EventTypeChecker<CallReturnEvent> CALL_RETURN_CHECKER = new EventTypeChecker<CallReturnEvent>(Event.Type.CALL_RETURN);
        public static final EventTypeChecker<CallThrowsEvent> CALL_THROWS_CHECKER = new EventTypeChecker<CallThrowsEvent>(Event.Type.CALL_THROWS);
        public static final EventTypeChecker<LineEvent> LINE_CHECKER = new EventTypeChecker<LineEvent>(Event.Type.LINE);

    }

}
