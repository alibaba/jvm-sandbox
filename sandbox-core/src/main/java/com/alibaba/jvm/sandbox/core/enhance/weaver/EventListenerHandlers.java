package com.alibaba.jvm.sandbox.core.enhance.weaver;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.enhance.annotation.Interrupted;
import com.alibaba.jvm.sandbox.core.util.EventPool;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import com.alibaba.jvm.sandbox.core.util.Sequencer;
import com.alibaba.jvm.sandbox.core.util.collection.GaStack;
import com.alibaba.jvm.sandbox.core.util.collection.ThreadUnsafeGaStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.com.alibaba.jvm.sandbox.spy.Spy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * 事件处理
 *
 * @author luanjia@taobao.com
 */
public class EventListenerHandlers {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 调用序列生成器
    private final Sequencer invokeIdSequencer = new Sequencer(1000);

    // 全局处理器ID:处理器映射集合
    private final Map<Integer/*LISTENER_ID*/, EventListenerWrap> globalEventListenerMap
            = new ConcurrentHashMap<Integer, EventListenerWrap>();

    // 事件对象池
    private final EventPool eventPool = new EventPool();

    /**
     * 获取事件对象池
     *
     * @return 事件对象池
     */
    public EventPool getEventPool() {
        return eventPool;
    }

    /**
     * 注册事件处理器
     *
     * @param listenerId     事件监听器ID
     * @param listener       事件监听器
     * @param eventTypeArray 监听事件集合
     */
    public void active(final int listenerId,
                       final EventListener listener,
                       final Event.Type[] eventTypeArray) {
        final EventListenerWrap wrap = new EventListenerWrap(listenerId, listener, eventTypeArray);
        globalEventListenerMap.put(listenerId, wrap);
        logger.info("activated listener[id={};target={};] event={}",
                listenerId,
                listener,
                join(eventTypeArray, ",")
        );
    }

    /**
     * 取消事件处理器
     *
     * @param listenerId 事件处理器ID
     */
    public void frozen(int listenerId) {
        final EventListenerWrap wrap = globalEventListenerMap.remove(listenerId);
        if (null == wrap) {
            logger.debug("ignore frozen listener[id={};], because not found.");
            return;
        }

        logger.info("frozen listener[id={};target={};]",
                listenerId,
                wrap.listener
        );
    }

    /**
     * 调用出发事件处理&调用执行流程控制
     *
     * @param listenerId 处理器ID
     * @param processId  调用过程ID
     * @param invokeId   调用ID
     * @param event      调用事件
     * @param wrap       事件处理器封装
     * @return 处理返回结果
     * @throws Throwable 当出现未知异常时,且事件处理器为中断流程事件时抛出
     */
    private Spy.Ret handleEvent(final int listenerId,
                                final int processId,
                                final int invokeId,
                                final Event event,
                                final EventListenerWrap wrap) throws Throwable {
        final EventListener listener = wrap.listener;

        try {

            // 调用事件处理
            listener.onEvent(event);
            if (logger.isDebugEnabled()) {
                logger.debug("on-event: event|{}|{}|{}@listener|{}",
                        event.type,
                        processId,
                        invokeId,
                        listenerId
                );
            }

        }

        // 代码执行流程变更
        catch (ProcessControlException pce) {

            final ProcessControlException.State state = pce.getState();
            logger.debug("on-event: event|{}|{}|{};listener|{}, process-changed: {}. isIgnoreProcessEvent={};",
                    event.type,
                    processId,
                    invokeId,
                    listenerId,
                    state,
                    pce.isIgnoreProcessEvent()
            );

            final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();

            // 如果流程控制要求忽略后续处理所有事件，则需要在此处进行标记
            // 标记当前线程中、当前EventListener中需要主动忽略的ProcessId
            if (pce.isIgnoreProcessEvent()) {
                eventProcess.markIgnoreProcessEvent(processId);
            }

            switch (state) {

                // 立即返回对象
                case RETURN_IMMEDIATELY: {

                    // 将BEFORE压入的堆栈弹出
                    if (event instanceof BeforeEvent) {
                        final GaStack<Integer> stack = wrap.eventProcessRef.get().processStack;
                        stack.pop();
                        wrap.cleanIfLast();
                        if (logger.isDebugEnabled()) {
                            logger.debug("invoke-stack mock pop, IMMEDIATELY-RETURN deep={};listener={};pid={};iid={}",
                                    stack.deep(),
                                    listenerId,
                                    processId,
                                    invokeId
                            );
                        }
                    }

                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        return Spy.Ret.newInstanceForReturn(pce.getRespond());
                    }

                    final ReturnEvent replaceReturnEvent = eventPool.borrowReturnEvent(processId, invokeId, pce.getRespond());
                    final Spy.Ret ret;
                    try {
                        ret = handleEvent(
                                listenerId,
                                processId,
                                invokeId,
                                replaceReturnEvent,
                                wrap
                        );
                    } finally {
                        eventPool.returnEvent(replaceReturnEvent);
                    }

                    if (ret.state == Spy.Ret.RET_STATE_NONE) {
                        return Spy.Ret.newInstanceForReturn(pce.getRespond());
                    } else {
                        // 如果不是,则返回最新的处理结果
                        return ret;
                    }

                }

                // 立即抛出异常
                case THROWS_IMMEDIATELY: {

                    // 将BEFORE压入的堆栈弹出
                    if (event instanceof BeforeEvent) {
                        final GaStack<Integer> stack = wrap.eventProcessRef.get().processStack;
                        stack.pop();
                        wrap.cleanIfLast();
                        if (logger.isDebugEnabled()) {
                            logger.debug("invoke-stack mock pop, IMMEDIATELY-THROWS deep={};listener={};pid={};iid={}",
                                    stack.deep(),
                                    listenerId,
                                    processId,
                                    invokeId
                            );
                        }
                    }

                    final Throwable throwable = (Throwable) pce.getRespond();

                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        return Spy.Ret.newInstanceForThrows(throwable);
                    }

                    if (!(event instanceof BeforeEvent)) {

                        final ThrowsEvent replaceThrowsEvent = eventPool.borrowThrowsEvent(processId, invokeId, throwable);
                        final Spy.Ret ret;
                        try {
                            ret = handleEvent(
                                    listenerId,
                                    processId,
                                    invokeId,
                                    replaceThrowsEvent,
                                    wrap
                            );
                        } finally {
                            eventPool.returnEvent(replaceThrowsEvent);
                        }

                        if (ret.state == Spy.Ret.RET_STATE_NONE) {
                            return Spy.Ret.newInstanceForThrows(throwable);
                        } else {
                            // 如果不是,则返回最新的处理结果
                            return ret;
                        }
                    } else {
                        return Spy.Ret.newInstanceForThrows(throwable);
                    }

                }

                // 什么都不操作，立即返回
                case NONE_IMMEDIATELY:
                default: {
                    return Spy.Ret.newInstanceForNone();
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
                logger.debug("on-event: event|{}|{}|{};listener|{} occur an error.",
                        event.type,
                        processId,
                        invokeId,
                        listenerId,
                        throwable
                );
            }
        }

        // 默认返回不进行任何流程变更
        return Spy.Ret.newInstanceForNone();
    }

    private boolean isInterruptEventHandler(final Class<? extends EventListener> listenerClass) {
        return listenerClass.isAnnotationPresent(Interrupted.class);
    }


    private final WeakHashMap<Class, Method> spyNewInstanceForNoneMethodCache = new WeakHashMap<Class, Method>();
    private final WeakHashMap<Class, Method> spyNewInstanceForReturnMethodCache = new WeakHashMap<Class, Method>();
    private final WeakHashMap<Class, Method> spyNewInstanceForThrowsMethodCache = new WeakHashMap<Class, Method>();

    // 转换当前的Spy.Ret到目标类所在ClassLoader的Spy.Ret
    private Object toSpyRetInTargetClassLoader(final Spy.Ret ret, final Class<?> spyRetClassInTargetClassLoader) throws Throwable {

        // 如果两个Spy.Ret的类相等，说明他们在同一个ClassLoader空间
        // 可以直接返回当前我们自己构造的Spy.Ret，不需要走如此复杂的反射
        if (Spy.Ret.class == spyRetClassInTargetClassLoader) {
            return ret;
        }

        // 如果当前Spy.Ret和目标ClassLoader中的Spy.Ret不一致，说明他们来自不同的ClassLoader空间
        // 此时就需要性能比较高昂的转换了，这里稍微加了几个MethodCache做了性能缓冲，尽量减少性能开销
        // 但此时我觉得必要性可能不大，后续考虑可以直接优化掉这个几个MethodCache
        switch (ret.state) {
            case Spy.Ret.RET_STATE_NONE: {

                final Method method;
                if (spyNewInstanceForNoneMethodCache.containsKey(spyRetClassInTargetClassLoader)) {
                    method = spyNewInstanceForNoneMethodCache.get(spyRetClassInTargetClassLoader);
                } else {
                    method = unCaughtGetClassDeclaredJavaMethod(spyRetClassInTargetClassLoader, "newInstanceForNone");
                    spyNewInstanceForNoneMethodCache.put(spyRetClassInTargetClassLoader, method);
                }
                return method.invoke(null);
            }

            case Spy.Ret.RET_STATE_RETURN: {
                final Method method;
                if (spyNewInstanceForReturnMethodCache.containsKey(spyRetClassInTargetClassLoader)) {
                    method = spyNewInstanceForReturnMethodCache.get(spyRetClassInTargetClassLoader);
                } else {
                    method = unCaughtGetClassDeclaredJavaMethod(spyRetClassInTargetClassLoader, "newInstanceForReturn", Object.class);
                    spyNewInstanceForReturnMethodCache.put(spyRetClassInTargetClassLoader, method);
                }
                return method.invoke(null, ret.respond);
            }

            case Spy.Ret.RET_STATE_THROWS: {
                final Method method;
                if (spyNewInstanceForThrowsMethodCache.containsKey(spyRetClassInTargetClassLoader)) {
                    method = spyNewInstanceForThrowsMethodCache.get(spyRetClassInTargetClassLoader);
                } else {
                    method = unCaughtGetClassDeclaredJavaMethod(spyRetClassInTargetClassLoader, "newInstanceForThrows", Throwable.class);
                    spyNewInstanceForThrowsMethodCache.put(spyRetClassInTargetClassLoader, method);
                }
                return method.invoke(null, (Throwable) ret.respond);
            }

            default: {
                throw new IllegalStateException("illegal Spy.Ret.state=" + ret.state);
            }
        }
    }

    private Spy.Ret handleOnBefore(final int listenerId,
                                   final ClassLoader javaClassLoader,
                                   final String javaClassName,
                                   final String javaMethodName,
                                   final String javaMethodDesc,
                                   final Object target,
                                   final Object[] argumentArray) throws Throwable {

        // 获取事件处理器
        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing before-event.", listenerId);
            return Spy.Ret.newInstanceForNone();
        }

        // 获取调用跟踪信息
        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;

        // 调用ID
        final int invokeId = invokeIdSequencer.next();

        // 调用过程ID
        final int processId = stack.isEmpty()
                ? invokeId
                : stack.peekLast();

        // 将当前调用压栈
        stack.push(invokeId);

        if (logger.isDebugEnabled()) {
            logger.debug("invoke-stack push, deep={};listener={};pid={};iid={}",
                    stack.deep(),
                    listenerId,
                    processId,
                    invokeId
            );
        }

        // 如果当前处理ID被忽略，则立即返回
        // 放在stack.push后边是为了对齐执行栈
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return Spy.Ret.newInstanceForNone();
        } else {
            eventProcess.cleanIgnoreProcessEvent();
        }

        final BeforeEvent event = eventPool.borrowBeforeEvent(
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
            return handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }
    }

    /*
     * 判断堆栈是否错位
     */
    private boolean isStackErrorPosition(final int processId,
                                         final int invokeId,
                                         final GaStack<Integer> stack) {
        return (processId == invokeId && !stack.isEmpty())
                || (processId != invokeId && stack.isEmpty());
    }

    private Spy.Ret handleOnEnd(final int listenerId,
                                final Object object,
                                final boolean isReturn) throws Throwable {

        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing return-event|throws-event.", listenerId);
            return Spy.Ret.newInstanceForNone();
        }

        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;

        // 如果当前调用过程信息堆栈是空的,说明
        // 1. BEFORE/RETURN错位
        // 2. super.<init>
        // 处理方式是直接返回,不做任何事件的处理和代码流程的改变,放弃对super.<init>的观察，可惜了
        if (stack.isEmpty()) {
            return Spy.Ret.newInstanceForNone();
        }

        final int processId = stack.peekLast();
        final int invokeId = stack.pop();
        wrap.cleanIfLast();

        if (logger.isDebugEnabled()) {
            logger.debug("invoke-stack pop, deep={};listener={};pid={};iid={}",
                    stack.deep(),
                    listenerId,
                    processId,
                    invokeId
            );
        }

        // 如果PID==IID说明已经到栈顶，此时需要核对堆栈是否为空
        // 如果不为空需要输出日志进行告警
        if (isStackErrorPosition(processId, invokeId, stack)) {
            logger.warn("stack error position. deep={};listener={};", stack.deep(), listenerId);
        }

        // 忽略事件处理
        // 放在stack.pop()后边是为了对齐执行栈
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return Spy.Ret.newInstanceForNone();
        }

        final Event event = isReturn
                ? eventPool.borrowReturnEvent(processId, invokeId, object)
                : eventPool.borrowThrowsEvent(processId, invokeId, (Throwable) object);

        try {
            return handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }

    }


    private Object handleOnBeforeWithTargetClassLoaderSpyRet(final int listenerId,
                                                             final ClassLoader javaClassLoader,
                                                             final Class<?> spyRetClassInTargetClassLoader,
                                                             final String javaClassName,
                                                             final String javaMethodName,
                                                             final String javaMethodDesc,
                                                             final Object target,
                                                             final Object[] argumentArray) throws Throwable {
        return toSpyRetInTargetClassLoader(
                handleOnBefore(
                        listenerId,
                        javaClassLoader,
                        javaClassName,
                        javaMethodName,
                        javaMethodDesc,
                        target,
                        argumentArray
                ),
                spyRetClassInTargetClassLoader
        );
    }

    private Object handleOnReturnWithTargetClassLoaderSpyRet(final int listenerId,
                                                             final Class<?> spyRetClassInTargetClassLoader,
                                                             final Object object) throws Throwable {
        return toSpyRetInTargetClassLoader(handleOnEnd(listenerId, object, true), spyRetClassInTargetClassLoader);
    }

    private Object handleOnThrowsWithTargetClassLoaderSpyRet(final int listenerId,
                                                             final Class<?> spyRetClassInTargetClassLoader,
                                                             final Throwable throwable) throws Throwable {
        return toSpyRetInTargetClassLoader(handleOnEnd(listenerId, throwable, false), spyRetClassInTargetClassLoader);
    }

    private void handleOnLine(final int listenerId,
                              final int lineNumber) throws Throwable {
        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing line-event.", listenerId);
            return;
        }

        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;

        // 如果当前调用过程信息堆栈是空的,说明BEFORE/LINE错位
        // 处理方式是直接返回,不做任何事件的处理和代码流程的改变
        if (stack.isEmpty()) {
            return;
        }
        final int processId = stack.peekLast();
        final int invokeId = stack.peek();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return;
        }

        final Event event = eventPool.borrowLineEvent(processId, invokeId, lineNumber);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }

    }

    private void handleOnCallBefore(final int listenerId,
                                    final int lineNumber,
                                    final String owner,
                                    final String name,
                                    final String desc) throws Throwable {
        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-before-event.", listenerId);
            return;
        }

        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;


        // 如果当前调用过程信息堆栈是空的,有两种情况
        // 1. CALL_BEFORE事件和BEFORE事件错位
        // 2. 当前方法是<init>，而CALL_BEFORE事件触发是当前方法在调用父类的<init>
        //    super.<init>会导致CALL_BEFORE事件优先于BEFORE事件
        // 但如果按照现在的架构要兼容这种情况，比较麻烦，所以暂时先放弃了这部分的消息，可惜可惜
        if (stack.isEmpty()) {
            return;
        }

        final int processId = stack.peekLast();
        final int invokeId = stack.peek();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return;
        }

        final Event event = eventPool.borrowCallBeforeEvent(processId, invokeId, lineNumber, owner, name, desc);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }

    }

    private void handleOnCallReturn(final int listenerId) throws Throwable {

        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-return-event.", listenerId);
            return;
        }

        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;
        if (stack.isEmpty()) {
            return;
        }

        final int processId = stack.peekLast();
        final int invokeId = stack.peek();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return;
        }

        final Event event = eventPool.borrowCallReturnEvent(processId, invokeId);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }

    }

    private void handleOnCallThrows(final int listenerId,
                                    final String throwException) throws Throwable {
        final EventListenerWrap wrap = globalEventListenerMap.get(listenerId);
        if (null == wrap) {
            logger.debug("listener={} is not activated, ignore processing call-throws-event.", listenerId);
            return;
        }

        final EventListenerWrap.EventProcess eventProcess = wrap.eventProcessRef.get();
        final GaStack<Integer> stack = eventProcess.processStack;
        if (stack.isEmpty()) {
            return;
        }

        final int processId = stack.peekLast();
        final int invokeId = stack.peek();

        // 如果事件处理流被忽略，则直接返回，不产生后续事件
        if (eventProcess.isIgnoreProcessEvent(processId)) {
            return;
        }

        final Event event = eventPool.borrowCallThrowsEvent(processId, invokeId, throwException);
        try {
            handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            eventPool.returnEvent(event);
        }
    }

    /**
     * 事件处理器封装
     */
    private final class EventListenerWrap {

        class EventProcess {

            final GaStack<Integer> processStack = new ThreadUnsafeGaStack<Integer>();
            Integer ignoreProcessId = null;

            boolean isIgnoreProcessEvent(int targetProcessId) {
                return ignoreProcessId != null
                        && ignoreProcessId == targetProcessId;
            }

            void markIgnoreProcessEvent(int processId) {
                this.ignoreProcessId = processId;
            }

            void cleanIgnoreProcessEvent() {
                this.ignoreProcessId = null;
            }

        }

        private final int listenerId;
        private final EventListener listener;
        private final ThreadLocal<EventProcess> eventProcessRef = new ThreadLocal<EventProcess>() {
            @Override
            protected EventProcess initialValue() {
                return new EventProcess();
            }
        };

        private EventListenerWrap(final int listenerId,
                                  final EventListener listener,
                                  final Event.Type[] eventTypeArray) {

            this.listenerId = listenerId;

            if (isInterruptEventHandler(listener.getClass())) {
                this.listener = new InterruptedEventListenerImpl(
                        new SeparateImmediatelyEventListener(listener, eventTypeArray, eventPool)
                );
            } else {
                this.listener = new SeparateImmediatelyEventListener(listener, eventTypeArray, eventPool);
            }
        }

        void cleanIfLast() {
            final EventProcess eventProcess = eventProcessRef.get();
            if (eventProcess.processStack.isEmpty()) {
                eventProcessRef.remove();
                logger.debug("clean TLS: listener-wrap, listener={};", listenerId);
            }
        }

    }

    @Interrupted
    private class InterruptedEventListenerImpl implements EventListener {

        private final EventListener listener;

        private InterruptedEventListenerImpl(EventListener listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(Event event) throws Throwable {
            listener.onEvent(event);
        }

    }


    // ----------------------------------- 从这里开始就是提供给Spy的static方法 -----------------------------------

    private static EventListenerHandlers singleton = new EventListenerHandlers();

    public static EventListenerHandlers getSingleton() {
        return singleton;
    }


    public static Object onBefore(final int listenerId,
                                  final int targetClassLoaderObjectID,
                                  final Class<?> spyRetClassInTargetClassLoader,
                                  final String javaClassName,
                                  final String javaMethodName,
                                  final String javaMethodDesc,
                                  final Object target,
                                  final Object[] argumentArray) throws Throwable {
        return singleton.handleOnBeforeWithTargetClassLoaderSpyRet(
                listenerId,
                (ClassLoader) ObjectIDs.instance.getObject(targetClassLoaderObjectID),
                spyRetClassInTargetClassLoader,
                javaClassName,
                javaMethodName,
                javaMethodDesc,
                target,
                argumentArray
        );
    }

    public static Object onReturn(final int listenerId,
                                  final Class<?> spyRetClassInTargetClassLoader,
                                  final Object object) throws Throwable {
        return singleton.handleOnReturnWithTargetClassLoaderSpyRet(listenerId, spyRetClassInTargetClassLoader, object);
    }

    public static Object onThrows(final int listenerId,
                                  final Class<?> spyRetClassInTargetClassLoader,
                                  final Throwable throwable) throws Throwable {
        return singleton.handleOnThrowsWithTargetClassLoaderSpyRet(listenerId, spyRetClassInTargetClassLoader, throwable);
    }

    public static void onLine(final int listenerId,
                              final int lineNumber) throws Throwable {
        singleton.handleOnLine(listenerId, lineNumber);
    }

    public static void onCallBefore(final int listenerId,
                                    final int lineNumber,
                                    final String owner,
                                    final String name,
                                    final String desc) throws Throwable {
        singleton.handleOnCallBefore(
                listenerId,
                lineNumber,
                owner,
                name,
                desc
        );
    }

    public static void onCallReturn(final int listenerId) throws Throwable {
        singleton.handleOnCallReturn(listenerId);
    }

    public static void onCallThrows(final int listenerId,
                                    final String throwException) throws Throwable {
        singleton.handleOnCallThrows(listenerId, throwException);
    }


}
