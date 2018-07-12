package com.alibaba.jvm.sandbox.core.enhance.weaver;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.util.EventPool;
import org.apache.commons.lang3.ArrayUtils;

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
            return Step.STEP_ORIGINAL_EVENT;
        }
    };

    private final EventListener listener;
    private final EventPool eventPool;
    private final Event.Type[] eventTypeArray;

    public SeparateImmediatelyEventListener(final Event.Type[] eventTypeArray,
                                            final EventListener listener,
                                            final EventPool eventPool) {
        this.listener = listener;
        this.eventPool = eventPool;
        this.eventTypeArray = eventTypeArray;
    }

    @Override
    public void onEvent(final Event event) throws Throwable {

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

        try {
            stepRef.set(Step.STEP_ORIGINAL_EVENT);

            // 如果当前事件不在事件监听范围,则直接忽略什么都不用处理
            if (!ArrayUtils.contains(eventTypeArray, replaceEvent.type)) {
                return;
            }

            // 处理事件
            listener.onEvent(replaceEvent);

        } catch (ProcessControlException pce) {

            switch (pce.getState()) {
                case RETURN_IMMEDIATELY: {
                    stepRef.set(Step.STEP_IMMEDIATELY_RETURN_EVENT);
                    break;
                }
                case THROWS_IMMEDIATELY: {
                    stepRef.set(Step.STEP_IMMEDIATELY_THROWS_EVENT);
                    break;
                }
                case NONE_IMMEDIATELY:
                default: {
                    stepRef.set(Step.STEP_ORIGINAL_EVENT);
                    break;
                }
            }

            throw pce;

        } finally {
            if (replaceEvent instanceof ImmediatelyReturnEvent
                    || replaceEvent instanceof ImmediatelyThrowsEvent) {
                eventPool.returnEvent(replaceEvent);
            }
        }

    }

}
