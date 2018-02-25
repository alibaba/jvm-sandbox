package com.alibaba.jvm.sandbox.api;

import static com.alibaba.jvm.sandbox.api.ProcessControlException.State.RETURN_IMMEDIATELY;
import static com.alibaba.jvm.sandbox.api.ProcessControlException.State.THROWS_IMMEDIATELY;

/**
 * 流程控制异常
 * <p>用于控制事件处理器处理事件走向</p>
 *
 * @author luanjia@taobao.com
 */
public final class ProcessControlException extends Exception {

    // 流程控制状态
    private final State state;

    // 回应结果对象(直接返回或者抛出异常)
    private final Object respond;

    private ProcessControlException(State state, Object respond) {
        this.state = state;
        this.respond = respond;
    }

    /**
     * 中断当前代码处理流程,并立即返回指定对象
     *
     * @param object 返回对象
     * @throws ProcessControlException 抛出立即返回流程控制异常
     */
    public static void throwReturnImmediately(final Object object) throws ProcessControlException {
        throw new ProcessControlException(RETURN_IMMEDIATELY, object);
    }

    /**
     * 中断当前代码处理流程,并抛出指定异常
     *
     * @param throwable 指定异常
     * @throws ProcessControlException 抛出立即抛出异常流程控制异常
     */
    public static void throwThrowsImmediately(final Throwable throwable) throws ProcessControlException {
        throw new ProcessControlException(THROWS_IMMEDIATELY, throwable);
    }

    public State getState() {
        return state;
    }

    public Object getRespond() {
        return respond;
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }


    /**
     * 流程控制状态
     */
    public enum State {

        /**
         * 立即返回
         */
        RETURN_IMMEDIATELY,

        /**
         * 立即抛出异常
         */
        THROWS_IMMEDIATELY,

    }

}
