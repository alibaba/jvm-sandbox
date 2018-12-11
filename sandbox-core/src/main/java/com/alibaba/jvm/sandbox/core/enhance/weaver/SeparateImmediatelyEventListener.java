package com.alibaba.jvm.sandbox.core.enhance.weaver;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.util.EventPool;
import org.apache.commons.lang3.ArrayUtils;

import static com.alibaba.jvm.sandbox.core.enhance.weaver.SeparateImmediatelyEventListener.Step.*;

/**
 * 用于分离"立即返回"／"返回"和"立即异常抛出事件"／"异常抛出事件"
 * Created by luanjia@taobao.com on 2017/2/26.
 */
public class SeparateImmediatelyEventListener implements EventListener {

    enum Step {
        STEP_IMMEDIATELY_RETURN_EVENT,
        STEP_IMMEDIATELY_THROWS_EVENT,
        STEP_ORIGINAL_EVENT
    }

    private final ThreadLocal<Step> stepRef = new ThreadLocal<Step>() {
        @Override
        protected Step initialValue() {
            return STEP_ORIGINAL_EVENT;
        }
    };


    private final EventListener listener;
    private final EventPool eventPool;
    private final Event.Type[] eventTypeArray;

    public SeparateImmediatelyEventListener(final EventListener listener,
                                            final Event.Type[] eventTypeArray,
                                            final EventPool eventPool) {
        this.listener = listener;
        this.eventPool = eventPool;
        this.eventTypeArray = eventTypeArray;
    }

    @Override
    public void onEvent(final Event event) throws Throwable {

        // 只有BEFORE/RETURN/THROWS事件才需要进行分离
        if (!(event instanceof BeforeEvent)
                && !(event instanceof ReturnEvent)
                && !(event instanceof ThrowsEvent)) {
            if(!ArrayUtils.contains(eventTypeArray, event.type)) {
                return;
            }
            listener.onEvent(event);
            return;
        }

        // 分离Immediately事件
        final Event replaceEvent;
        final Step step = stepRef.get();
        switch (step) {
            case STEP_IMMEDIATELY_RETURN_EVENT: {
                final ReturnEvent returnEvent = (ReturnEvent) event;
                replaceEvent = eventPool.borrowImmediatelyReturnEvent(returnEvent.processId, returnEvent.invokeId, returnEvent.object);
                break;
            }
            case STEP_IMMEDIATELY_THROWS_EVENT: {
                final ThrowsEvent throwsEvent = (ThrowsEvent) event;
                replaceEvent = eventPool.borrowImmediatelyThrowsEvent(throwsEvent.processId, throwsEvent.invokeId, throwsEvent.throwable);
                break;
            }
            case STEP_ORIGINAL_EVENT:
            default: {
                replaceEvent = event;
                break;
            }
        }

        // 驱动分离后的事件
        try {
            stepRef.set(STEP_ORIGINAL_EVENT);

            // 如果当前事件(分离之后)不在事件监听范围,则直接忽略什么都不用处理
            if (!ArrayUtils.contains(eventTypeArray, replaceEvent.type)) {
                return;
            }

            // 处理事件
            listener.onEvent(replaceEvent);

        } catch (ProcessControlException pce) {

            switch (pce.getState()) {
                case RETURN_IMMEDIATELY: {
                    stepRef.set(STEP_IMMEDIATELY_RETURN_EVENT);
                    break;
                }
                case THROWS_IMMEDIATELY: {
                    stepRef.set(STEP_IMMEDIATELY_THROWS_EVENT);
                    break;
                }
                case NONE_IMMEDIATELY:
                default: {
                    stepRef.set(STEP_ORIGINAL_EVENT);
                    break;
                }
            }

            throw pce;

        } finally {

            // 发生了事件分离才需要归还
            if (replaceEvent != event
                    && (replaceEvent instanceof ImmediatelyReturnEvent || replaceEvent instanceof ImmediatelyThrowsEvent)) {
                eventPool.returnEvent(replaceEvent);
            }

        }

    }

}
