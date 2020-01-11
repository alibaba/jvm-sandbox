package com.alibaba.jvm.sandbox.core.enhance.weaver;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.InvokeEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.com.alibaba.jvm.sandbox.spy.Spy;
import java.com.alibaba.jvm.sandbox.spy.SpyHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.IMMEDIATELY_RETURN;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.IMMEDIATELY_THROWS;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.isInterruptEventHandler;
import static java.com.alibaba.jvm.sandbox.spy.Spy.Ret.newInstanceForNone;
import static java.com.alibaba.jvm.sandbox.spy.Spy.Ret.newInstanceForThrows;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * 事件处理
 *
 * @author luanjia@taobao.com
 */
public class EventListenerHandler implements SpyHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 调用序列生成器
    // private final Sequencer invokeIdSequencer = new Sequencer();
    private final AtomicInteger invokeIdSequencer = new AtomicInteger(1000);

    // 全局处理器ID:处理器映射集合
    private final Map<Integer/*LISTENER_ID*/, EventProcessor> mappingOfEventProcessor
            = new ConcurrentHashMap<Integer, EventProcessor>();

    /**
     * 注册事件处理器
     *
     * @param listenerId 事件监听器ID
     * @param listener   事件监听器
     * @param eventTypes 监听事件集合
     */
    public void active(final int listenerId,
                       final EventListener listener,
                       final Event.Type[] eventTypes) {
        mappingOfEventProcessor.put(listenerId, new EventProcessor(listenerId, listener, eventTypes));
        logger.info("activated listener[id={};target={};] event={}",
                listenerId,
                listener,
                join(eventTypes, ",")
        );
    }

    /**
     * 取消事件处理器
     *
     * @param listenerId 事件处理器ID
     */
    public void frozen(int listenerId) {
        final EventProcessor processor = mappingOfEventProcessor.remove(listenerId);
        if (null == processor) {
            logger.debug("ignore frozen listener={}, because not found.", listenerId);
            return;
        }

        logger.info("frozen listener[id={};target={};]",
                listenerId,
                processor.listener
        );

        // processor.clean();
    }

    /**
     * 调用出发事件处理&调用执行流程控制
     *
     * @param listenerId 处理器ID
     * @param processId  调用过程ID
     * @param invokeId   调用ID
     * @param event      调用事件
     * @param processor  事件处理器
     * @return 处理返回结果
     * @throws Throwable 当出现未知异常时,且事件处理器为中断流程事件时抛出
     */
    private Spy.Ret handleEvent(final int listenerId,
                                final int processId,
                                final int invokeId,
                                final Event event,
                                final EventProcessor processor) throws Throwable {
        // 获取事件监听器
        final EventListener listener = processor.listener;

        // 如果当前事件不在事件监听器处理列表中，则直接返回，不处理事件
        if (!contains(processor.eventTypes, event.type)) {
            return newInstanceForNone();
        }

        // 调用事件处理
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("on-event: event|{}|{}|{}|{}",
                        event.type,
                        processId,
                        invokeId,
                        listenerId
                );
            }
            listener.onEvent(event);
        }

        // 代码执行流程变更
        catch (ProcessControlException pce) {

            final EventProcessor.Process process = processor.processRef.get();

            final ProcessControlException.State state = pce.getState();
            logger.debug("on-event: event|{}|{}|{}|{}, process-changed: {}. isIgnoreProcessEvent={};",
                    event.type,
                    processId,
                    invokeId,
                    listenerId,
                    state,
                    pce.isIgnoreProcessEvent()
            );

            // 如果流程控制要求忽略后续处理所有事件，则需要在此处进行标记
            if (pce.isIgnoreProcessEvent()) {
                process.markIgnoreProcess();
            }

            switch (state) {

                // 立即返回对象
                case RETURN_IMMEDIATELY: {

                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        logger.debug("on-event: event|{}|{}|{}|{}, ignore immediately-return-event, isIgnored.",
                                event.type,
                                processId,
                                invokeId,
                                listenerId
                        );
                    } else {
                        // 补偿立即返回事件
                        compensateProcessControlEvent(pce, processor, process, event);
                    }

                    // 如果是在BEFORE中立即返回，则后续不会再有RETURN事件产生
                    // 这里需要主动对齐堆栈
                    if (event.type == Event.Type.BEFORE) {
                        process.popInvokeId();
                    }

                    // 让流程立即返回
                    return Spy.Ret.newInstanceForReturn(pce.getRespond());

                }

                // 立即抛出异常
                case THROWS_IMMEDIATELY: {

                    final Throwable throwable = (Throwable) pce.getRespond();

                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        logger.debug("on-event: event|{}|{}|{}|{}, ignore immediately-throws-event, isIgnored.",
                                event.type,
                                processId,
                                invokeId,
                                listenerId
                        );
                    } else {

                        // 如果是在BEFORE中立即抛出，则后续不会再有THROWS事件产生
                        // 这里需要主动对齐堆栈
                        if (event.type == Event.Type.BEFORE) {
                            process.popInvokeId();
                        }

                        // 标记本次异常由ImmediatelyException产生，让下次异常事件处理直接忽略
                        if (event.type != Event.Type.THROWS) {
                            process.markExceptionFromImmediately();
                        }

                        // 补偿立即抛出事件
                        compensateProcessControlEvent(pce, processor, process, event);
                    }

                    // 让流程立即抛出
                    return Spy.Ret.newInstanceForThrows(throwable);

                }

                // 什么都不操作，立即返回
                case NONE_IMMEDIATELY:
                default: {
                    return newInstanceForNone();
                }
            }

        }

        // BEFORE处理异常,打日志,并通知下游不需要进行处理
        catch (Throwable throwable) {

            // 如果当前事件处理器是可中断的事件处理器,则对外抛出UnCaughtException
            // 中断当前方法
            if (isInterruptEventHandler(listener.getClass())) {
                throw throwable;
            }

            // 普通事件处理器则可以打个日志后,直接放行
            else {
                logger.warn("on-event: event|{}|{}|{}|{} occur an error.",
                        event.type,
                        processId,
                        invokeId,
                        listenerId,
                        throwable
                );
            }
        }

        // 默认返回不进行任何流程变更
        return newInstanceForNone();
    }

    // 补偿事件
    // 随着历史版本的演进，一些事件已经过期，但为了兼容API，需要在这里进行补偿
    private void compensateProcessControlEvent(ProcessControlException pce, EventProcessor processor, EventProcessor.Process process, Event event) {

        // 核对是否需要补偿，如果目标监听器没监听过这类事件，则不需要进行补偿
        if (!(event instanceof InvokeEvent)
                || !contains(processor.eventTypes, event.type)) {
            return;
        }

        final InvokeEvent iEvent = (InvokeEvent) event;
        final Event compensateEvent;

        // 补偿立即返回事件
        if (pce.getState() == ProcessControlException.State.RETURN_IMMEDIATELY
                && contains(processor.eventTypes, IMMEDIATELY_RETURN)) {
            compensateEvent = process
                    .getEventFactory()
                    .makeImmediatelyReturnEvent(iEvent.processId, iEvent.invokeId, pce.getRespond());
        }

        // 补偿立即抛出事件
        else if (pce.getState() == ProcessControlException.State.THROWS_IMMEDIATELY
                && contains(processor.eventTypes, IMMEDIATELY_THROWS)) {
            compensateEvent = process
                    .getEventFactory()
                    .makeImmediatelyThrowsEvent(iEvent.processId, iEvent.invokeId, (Throwable) pce.getRespond());
        }

        // 异常情况不补偿
        else {
            return;
        }

        try {
            logger.debug("compensate-event: event|{}|{}|{}|{} when ori-event:{}",
                    compensateEvent.type,
                    iEvent.processId,
                    iEvent.invokeId,
                    processor.listenerId,
                    event.type
            );
            processor.listener.onEvent(compensateEvent);
        } catch (Throwable cause) {
            logger.warn("compensate-event: event|{}|{}|{}|{} when ori-event:{} occur error.",
                    compensateEvent.type,
                    iEvent.processId,
                    iEvent.invokeId,
                    processor.listenerId,
                    event.type,
                    cause
            );
        } finally {
            process.getEventFactory().returnEvent(compensateEvent);
        }
    }

    /*
     * 判断堆栈是否错位
     */
    private boolean checkProcessStack(final int processId,
                                      final int invokeId,
                                      final boolean isEmptyStack) {
        return (processId == invokeId && !isEmptyStack)
                || (processId != invokeId && isEmptyStack);
    }

    @Override
    public Spy.Ret handleOnBefore(int listenerId, int targetClassLoaderObjectID, Object[] argumentArray, String javaClassName, String javaMethodName, String javaMethodDesc, Object target) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing before-event", listenerId);
            return newInstanceForNone();
        }

        // 获取事件处理器
        final EventProcessor processor = mappingOfEventProcessor.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == processor) {
            logger.debug("listener={} is not activated, ignore processing before-event.", listenerId);
            return newInstanceForNone();
        }

        // 获取调用跟踪信息
        final EventProcessor.Process process = processor.processRef.get();

        // 如果当前处理ID被忽略，则立即返回
        if (process.isIgnoreProcess()) {
            logger.debug("listener={} is marked ignore process!", listenerId);
            return newInstanceForNone();
        }

        // 调用ID
        final int invokeId = invokeIdSequencer.getAndIncrement();
        process.pushInvokeId(invokeId);

        // 调用过程ID
        final int processId = process.getProcessId();

        final ClassLoader javaClassLoader = ObjectIDs.instance.getObject(targetClassLoaderObjectID);
        final BeforeEvent event = process.getEventFactory().makeBeforeEvent(
                processId,
                invokeId,
                javaClassLoader,
                javaClassName,
                javaMethodName,
                javaMethodDesc,
                target,
                argumentArray
        );
        try {
            return handleEvent(listenerId, processId, invokeId, event, processor);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    @Override
    public Spy.Ret handleOnThrows(int listenerId, Throwable throwable) throws Throwable {
        return handleOnEnd(listenerId, throwable, false);
    }

    @Override
    public Spy.Ret handleOnReturn(int listenerId, Object object) throws Throwable {
        return handleOnEnd(listenerId, object, true);
    }


    private Spy.Ret handleOnEnd(final int listenerId,
                                final Object object,
                                final boolean isReturn) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing {}-event", listenerId, isReturn ? "return" : "throws");
            return newInstanceForNone();
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing return-event|throws-event.", listenerId);
            return newInstanceForNone();
        }

        final EventProcessor.Process process = wrap.processRef.get();

        // 如果当前调用过程信息堆栈是空的,说明
        // 1. BEFORE/RETURN错位
        // 2. super.<init>
        // 处理方式是直接返回,不做任何事件的处理和代码流程的改变,放弃对super.<init>的观察，可惜了
        if (process.isEmptyStack()) {
            return newInstanceForNone();
        }

        // 如果异常来自于ImmediatelyException，则忽略处理直接返回抛异常
        final boolean isExceptionFromImmediately = !isReturn && process.rollingIsExceptionFromImmediately();
        if (isExceptionFromImmediately) {
            return newInstanceForThrows((Throwable) object);
        }

        // 继续异常处理
        final int processId = process.getProcessId();
        final int invokeId = process.popInvokeId();

        // 忽略事件处理
        // 放在stack.pop()后边是为了对齐执行栈
        if (process.isIgnoreProcess()) {
            return newInstanceForNone();
        }

        // 如果PID==IID说明已经到栈顶，此时需要核对堆栈是否为空
        // 如果不为空需要输出日志进行告警
        if (checkProcessStack(processId, invokeId, process.isEmptyStack())) {
            logger.warn("ERROR process-stack. pid={};iid={};listener={};",
                    processId,
                    invokeId,
                    listenerId
            );
        }

        final Event event = isReturn
                ? process.getEventFactory().makeReturnEvent(processId, invokeId, object)
                : process.getEventFactory().makeThrowsEvent(processId, invokeId, (Throwable) object);

        try {
            return handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }

    }


    @Override
    public void handleOnCallBefore(int listenerId, int lineNumber, String owner, String name, String desc) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing call-before-event", listenerId);
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-before-event.", listenerId);
            return;
        }

        final EventProcessor.Process process = wrap.processRef.get();

        // 如果当前调用过程信息堆栈是空的,有两种情况
        // 1. CALL_BEFORE事件和BEFORE事件错位
        // 2. 当前方法是<init>，而CALL_BEFORE事件触发是当前方法在调用父类的<init>
        //    super.<init>会导致CALL_BEFORE事件优先于BEFORE事件
        // 但如果按照现在的架构要兼容这种情况，比较麻烦，所以暂时先放弃了这部分的消息，可惜可惜
        if (process.isEmptyStack()) {
            return;
        }

        final int processId = process.getProcessId();
        final int invokeId = process.getInvokeId();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (process.isIgnoreProcess()) {
            return;
        }

        final Event event = process
                .getEventFactory()
                .makeCallBeforeEvent(processId, invokeId, lineNumber, owner, name, desc);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    @Override
    public void handleOnCallReturn(int listenerId) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing call-return-event", listenerId);
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-return-event.", listenerId);
            return;
        }

        final EventProcessor.Process process = wrap.processRef.get();
        if (process.isEmptyStack()) {
            return;
        }

        final int processId = process.getProcessId();
        final int invokeId = process.getInvokeId();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (process.isIgnoreProcess()) {
            return;
        }

        final Event event = process
                .getEventFactory()
                .makeCallReturnEvent(processId, invokeId);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    @Override
    public void handleOnCallThrows(int listenerId, String throwException) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing call-throws-event", listenerId);
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-throws-event.", listenerId);
            return;
        }

        final EventProcessor.Process process = wrap.processRef.get();
        if (process.isEmptyStack()) {
            return;
        }

        final int processId = process.getProcessId();
        final int invokeId = process.getInvokeId();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (process.isIgnoreProcess()) {
            return;
        }

        final Event event = process
                .getEventFactory()
                .makeCallThrowsEvent(processId, invokeId, throwException);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    @Override
    public void handleOnLine(int listenerId, int lineNumber) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.debug("listener={} is in protecting, ignore processing call-line-event", listenerId);
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing line-event.", listenerId);
            return;
        }

        final EventProcessor.Process process = wrap.processRef.get();

        // 如果当前调用过程信息堆栈是空的,说明BEFORE/LINE错位
        // 处理方式是直接返回,不做任何事件的处理和代码流程的改变
        if (process.isEmptyStack()) {
            return;
        }

        final int processId = process.getProcessId();
        final int invokeId = process.getInvokeId();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (process.isIgnoreProcess()) {
            return;
        }

        final Event event = process.getEventFactory().makeLineEvent(processId, invokeId, lineNumber);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    // ---- 自检查
    public void checkEventProcessor(final int... listenerIds) {
        for (int listenerId : listenerIds) {
            final EventProcessor processor = mappingOfEventProcessor.get(listenerId);
            if (null == processor) {
                throw new IllegalStateException(String.format("listener=%s not existed.", listenerId));
            }
            processor.check();
        }
    }


    // ----------------------------------- 单例模式 -----------------------------------

    private static EventListenerHandler singleton = new EventListenerHandler();

    public static EventListenerHandler getSingleton() {
        return singleton;
    }


}
