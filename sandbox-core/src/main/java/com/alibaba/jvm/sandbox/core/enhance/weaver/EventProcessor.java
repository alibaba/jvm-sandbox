package com.alibaba.jvm.sandbox.core.enhance.weaver;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.enhance.annotation.Interrupted;
import com.alibaba.jvm.sandbox.core.util.collection.GaStack;
import com.alibaba.jvm.sandbox.core.util.collection.ThreadUnsafeGaStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.isInterruptEventHandler;

/**
 * 事件处理器
 */
class EventProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 处理单元
     */
    class Process {

        // 事件工厂
        private final SingleEventFactory eventFactory
                = new SingleEventFactory();

        // 调用堆栈
        private final GaStack<Integer> stack
                = new ThreadUnsafeGaStack<Integer>();

        // 是否需要忽略整个调用过程
        private boolean isIgnoreProcess = false;

        // 是否来自ImmediatelyThrowsException所抛出的异常
        private boolean isExceptionFromImmediately = false;

        /**
         * 压入调用ID
         *
         * @param invokeId 调用ID
         */
        void pushInvokeId(int invokeId) {
            stack.push(invokeId);
            if (logger.isDebugEnabled()) {
                logger.debug("push process-stack, process-id={};invoke-id={};deep={};listener={};",
                        stack.peekLast(),
                        invokeId,
                        stack.deep(),
                        listenerId
                );
            }
        }

        /**
         * 弹出调用ID
         *
         * @return 调用ID
         */
        int popInvokeId() {
            final int invokeId;
            if (logger.isDebugEnabled()) {
                final int processId = stack.peekLast();
                invokeId = stack.pop();
                logger.debug("pop process-stack, process-id={};invoke-id={};deep={};listener={};",
                        processId,
                        invokeId,
                        stack.deep(),
                        listenerId
                );
            } else {
                invokeId = stack.pop();
            }
            if (stack.isEmpty()) {
                processRef.remove();
                logger.debug("clean TLS: event-processor, listener={};", listenerId);
            }
            return invokeId;
        }

        /**
         * 获取调用ID
         *
         * @return 调用ID
         */
        int getInvokeId() {
            return stack.peek();
        }

        /**
         * 获取调用过程ID
         *
         * @return 调用过程ID
         */
        int getProcessId() {
            return stack.peekLast();
        }

        /**
         * 是否空堆栈
         *
         * @return TRUE:是；FALSE：否
         */
        boolean isEmptyStack() {
            return stack.isEmpty();
        }

        /**
         * 当前调用过程是否需要被忽略
         *
         * @return TRUE：需要忽略；FALSE：不需要忽略
         */
        boolean isIgnoreProcess() {
            return isIgnoreProcess;
        }

        /**
         * 标记调用过程需要被忽略
         */
        void markIgnoreProcess() {
            isIgnoreProcess = true;
        }

        /**
         * 判断当前异常是否来自于ImmediatelyThrowsException，
         * 如果当前的异常来自于ImmediatelyThrowsException，则会清空当前标志位
         *
         * @return TRUE:来自于；FALSE：不来自于
         */
        boolean rollingIsExceptionFromImmediately() {
            if (isExceptionFromImmediately) {
                isExceptionFromImmediately = false;
                return true;
            }
            return false;
        }

        /**
         * 标记当前调用异常来自于ImmediatelyThrowsException
         */
        void markExceptionFromImmediately() {
            isExceptionFromImmediately = true;
        }

        /**
         * 获取事件工厂
         *
         * @return 事件工厂
         */
        SingleEventFactory getEventFactory() {
            return eventFactory;
        }

    }

    @Interrupted
    private static class InterruptedEventListenerImpl implements EventListener {

        private final EventListener listener;

        private InterruptedEventListenerImpl(EventListener listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(Event event) throws Throwable {
            listener.onEvent(event);
        }

    }

    final int listenerId;
    final EventListener listener;
    final Event.Type[] eventTypes;
    final ThreadLocal<Process> processRef = new ThreadLocal<Process>() {
        @Override
        protected Process initialValue() {
            return new Process();
        }
    };

    EventProcessor(final int listenerId,
                   final EventListener listener,
                   final Event.Type[] eventTypes) {

        this.listenerId = listenerId;
        this.eventTypes = eventTypes;
        this.listener = isInterruptEventHandler(listener.getClass())
                ? new InterruptedEventListenerImpl(listener)
                : listener;
    }


    /**
     * 校验器，用于校验事件处理器状态是否正确
     * <p>用于测试用例</p>
     */
    class Checker {

        void check() {

            final EventProcessor.Process process = processRef.get();
            final ThreadUnsafeGaStack<Integer> stack = (ThreadUnsafeGaStack<Integer>) process.stack;

            if (!process.isEmptyStack()) {
                throw new IllegalStateException(String.format("process-stack is not empty! listener=%s;\n%s",
                        listenerId,
                        toString(stack)
                ));
            }

            for (int index = 0; index < stack.getElementArray().length; index++) {
                if (index <= stack.getCurrent()) {
                    if (null == stack.getElementArray()[index]) {
                        throw new IllegalStateException(String.format("process-stack element is null at index=[%d], listener=%s;\n%s",
                                index,
                                listenerId,
                                toString(stack)
                        ));
                    }
                } else {
                    if (null != stack.getElementArray()[index]) {
                        throw new IllegalStateException(String.format("process-stack element is not null at index=[%d], listener=%s;\n%s",
                                index,
                                listenerId,
                                toString(stack)
                        ));
                    }
                }
            }

            if (process.isIgnoreProcess) {
                throw new IllegalStateException(String.format("process isIgnoreProcess is not false!"));
            }


        }

        String toString(ThreadUnsafeGaStack<Integer> stack) {
            final StringBuilder stackSB = new StringBuilder(String.format("stack[deep=%d;current=%d;]{\n", stack.deep(), stack.getCurrent()));
            for (int index = 0; index < stack.getElementArray().length; index++) {
                stackSB.append("\t[").append(index).append("] = ").append(stack.getElementArray()[index]).append("\n");
            }
            stackSB.append("}");
            return stackSB.toString();
        }

    }

    /**
     * 校验事件处理器
     * <p>用于测试用例</p>
     */
    void check() {
        new Checker().check();
    }

}
