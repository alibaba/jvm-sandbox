package com.alibaba.jvm.sandbox.core.enhance.weaver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceAdapterListener;
import com.alibaba.jvm.sandbox.core.enhance.annotation.Interrupted;
import com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils;
import com.alibaba.jvm.sandbox.core.util.collection.GaStack;
import com.alibaba.jvm.sandbox.core.util.collection.ThreadUnsafeGaStack;

import org.eclipse.jetty.util.ConcurrentHashSet;
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

        // 需要忽略的过程ID
        private Integer ignoreProcessId = null;

        void pushInvokeId(int invokeId) {
            if (stack.isEmpty()) {
                //第一次进入
                EventProcessor.this.setcurrentThread();
            }
            stack.push(invokeId);

            logger.debug("push process-stack, invoke-id={};deep={};listener={};",
                    invokeId,
                    stack.deep(),
                    listenerId
            );
        }

        int popInvokeId() {
            final int invokeId = stack.pop();
            logger.debug("pop process-stack, invoke-id={};deep={};listener={};",
                    invokeId,
                    stack.deep(),
                    listenerId
            );
            if (stack.isEmpty()) {
                EventProcessor.this.clean();
                logger.debug("clean TLS: event-processor, listener={};", listenerId);
            }
            return invokeId;
        }

        int getInvokeId() {
            return stack.peek();
        }

        int getProcessId() {
            return stack.peekLast();
        }

        boolean isEmptyStack() {
            return stack.isEmpty();
        }

        boolean touchIsIgnoreProcess(int processId) {
            if (null != ignoreProcessId
                    && ignoreProcessId == processId) {
                return true;
            } else {
                ignoreProcessId = null;
                return false;
            }
        }

        void markIgnoreProcessId(int processId) {
            ignoreProcessId = processId;
        }

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
    int status;
    Map<Thread,Thread> currentThreadSet;
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
        this.status = 1;
        this.currentThreadSet = new ConcurrentHashMap(32);
    }

    /**
     *  将当前线程加入集合
     */
    public void setcurrentThread(){
        if(null != this.currentThreadSet){
            Thread t = Thread.currentThread();
            this.currentThreadSet.put(t,t);
        }
    }

    /**
     *  冻结process
     */
    public void frozen(){
        this.status = 0;
    }

    /**
     *  激活process
     */
    public void active(){
        this.status = 1;
    }

    /**
     *  当前processor是否被冻结
     */
    public boolean isFrozen(){
        return this.status == 0;
    }

    /**
     *  当前processor是否被激活
     */
    public boolean isActivated(){
        return this.status == 1;
    }

    /**
     *  方法结束时，清理
     */
    public void clean() {
        this.currentThreadSet.remove(Thread.currentThread());
        processRef.remove();
    }

    /**
     *  清理线程集合中的线程本地变量
     */
    public void cleanThreadLocal() {
        if(null != this.currentThreadSet){
            cleanThreadLocal(this.currentThreadSet);
        }
    }

    private void cleanThreadLocal(Map<Thread,Thread> threadSet) {
        try {
            Set<Thread> threads = threadSet.keySet();
            for(Thread thread : threads){
                //反射调用 ThreadLocal.ThreadLocalMap.remove(ThreadLocal)
                Object o = SandboxReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(Thread.class,"threadLocals",thread);
                if(null != o){
                    Method method = SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod(o.getClass(),"remove",ThreadLocal.class);
                    SandboxReflectUtils.unCaughtInvokeMethod(method,o,this.processRef);
                    //AdviceAdapterListener中的opStackRef也需要释放
                    if(this.listener instanceof AdviceAdapterListener){
                        Object opStackRef = SandboxReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(AdviceAdapterListener.class,"opStackRef",this.listener);
                        SandboxReflectUtils.unCaughtInvokeMethod(method,o,opStackRef);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("remove threadLocal error !",e);
        }
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

            if (null != process.ignoreProcessId) {
                throw new IllegalStateException(String.format("process ignoreProcessId is not null!, processId=%d", process.ignoreProcessId));
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
