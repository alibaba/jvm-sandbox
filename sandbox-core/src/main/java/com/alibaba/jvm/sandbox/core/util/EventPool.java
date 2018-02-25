package com.alibaba.jvm.sandbox.core.util;

import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

/**
 * 事件对象池
 *
 * @author luanjia@taobao.com
 */
public class EventPool {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final KeyedObjectPool<Event.Type, Event> pool;
    private final boolean isEnable;

    public EventPool() {
        this.pool = createEventPool();
        this.isEnable = this.pool != null;
    }

    private KeyedObjectPool<Event.Type, Event> createEventPool() {
        final CoreConfigure cfg = CoreConfigure.getInstance();
        if (cfg.isEventPoolEnable()) {
            final GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
            poolConfig.setMaxTotalPerKey(cfg.getEventPoolMaxTotalPerEvent());
            poolConfig.setMinIdlePerKey(cfg.getEventPoolMinIdlePerEvent());
            poolConfig.setMaxIdlePerKey(cfg.getEventPoolMaxIdlePerEvent());
            poolConfig.setMaxTotal(cfg.getEventPoolMaxTotal());
            logger.info("enable event-pool[per-key-idle-min={};per-key-idle-max={};per-key-max={};total={};]",
                    cfg.getEventPoolMinIdlePerEvent(),
                    cfg.getEventPoolMaxIdlePerEvent(),
                    cfg.getEventPoolMaxTotalPerEvent(),
                    cfg.getEventPoolMaxTotal()
            );
            return new GenericKeyedObjectPool<Event.Type, Event>(new EventFactory(), poolConfig);
        } else {
            logger.info("disable event-pool.");
            return null;
        }
    }

    public int getNumActive() {
        return isEnable
                ? pool.getNumActive()
                : -1;
    }

    public int getNumActive(Event.Type type) {
        return isEnable
                ? pool.getNumActive(type)
                : -1;
    }

    public int getNumIdle() {
        return isEnable
                ? pool.getNumIdle()
                : -1;
    }

    public int getNumIdle(Event.Type type) {
        return isEnable
                ? pool.getNumIdle(type)
                : -1;
    }

    public BeforeEvent borrowBeforeEvent(final int processId,
                                         final int invokeId,
                                         final ClassLoader javaClassLoader,
                                         final String javaClassName,
                                         final String javaMethodName,
                                         final String javaMethodDesc,
                                         final Object target,
                                         final Object[] argumentArray) {
        if (isEnable) {
            try {
                final BeforeEvent event = (BeforeEvent) pool.borrowObject(Event.Type.BEFORE);
                initBeforeEvent(
                        event,
                        processId, invokeId,
                        javaClassLoader, javaClassName, javaMethodName, javaMethodDesc,
                        target, argumentArray
                );
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow BeforeEvent[processId={};invokeId={};class={};method={};] failed.",
                        processId, invokeId, javaClassName, javaMethodName, cause);
            }
        }
        return new BeforeEvent(
                processId, invokeId,
                javaClassLoader, javaClassName, javaMethodName, javaMethodDesc,
                target, argumentArray
        );
    }

    public ReturnEvent borrowReturnEvent(final int processId,
                                         final int invokeId,
                                         final Object object) {
        if (isEnable) {
            try {
                final ReturnEvent event = (ReturnEvent) pool.borrowObject(Event.Type.RETURN);
                initReturnEvent(event, processId, invokeId, object);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow ReturnEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new ReturnEvent(processId, invokeId, object);
    }

    public ThrowsEvent borrowThrowsEvent(final int processId,
                                         final int invokeId,
                                         final Throwable throwable) {
        if (isEnable) {
            try {
                final ThrowsEvent event = (ThrowsEvent) pool.borrowObject(Event.Type.THROWS);
                initThrowsEvent(event, processId, invokeId, throwable);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow ThrowsEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new ThrowsEvent(processId, invokeId, throwable);
    }

    public LineEvent borrowLineEvent(final int processId,
                                     final int invokeId,
                                     final int lineNumber) {
        if (isEnable) {
            try {
                final LineEvent event = (LineEvent) pool.borrowObject(Event.Type.LINE);
                initLineEvent(event, processId, invokeId, lineNumber);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow LineEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new LineEvent(processId, invokeId, lineNumber);
    }

    public ImmediatelyReturnEvent borrowImmediatelyReturnEvent(final int processId,
                                                               final int invokeId,
                                                               final Object object) {
        if (isEnable) {
            try {
                final ImmediatelyReturnEvent event = (ImmediatelyReturnEvent) pool.borrowObject(Event.Type.IMMEDIATELY_RETURN);
                initReturnEvent(event, processId, invokeId, object);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow ImmediatelyReturnEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new ImmediatelyReturnEvent(processId, invokeId, object);
    }

    public ImmediatelyThrowsEvent borrowImmediatelyThrowsEvent(final int processId,
                                                               final int invokeId,
                                                               final Throwable throwable) {
        if (isEnable) {
            try {
                final ImmediatelyThrowsEvent event = (ImmediatelyThrowsEvent) pool.borrowObject(Event.Type.IMMEDIATELY_THROWS);
                initThrowsEvent(event, processId, invokeId, throwable);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow ImmediatelyThrowsEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new ImmediatelyThrowsEvent(processId, invokeId, throwable);
    }

    public CallBeforeEvent borrowCallBeforeEvent(final int processId,
                                                 final int invokeId,
                                                 final int lineNumber,
                                                 final String owner,
                                                 final String name,
                                                 final String desc) {
        if (isEnable) {
            try {
                final CallBeforeEvent event = (CallBeforeEvent) pool.borrowObject(Event.Type.CALL_BEFORE);
                initCallBeforeEvent(event, processId, invokeId, lineNumber, owner, name, desc);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow CallBeforeEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new CallBeforeEvent(processId, invokeId, lineNumber, owner, name, desc);
    }

    public CallReturnEvent borrowCallReturnEvent(final int processId,
                                                 final int invokeId) {
        if (isEnable) {
            try {
                final CallReturnEvent event = (CallReturnEvent) pool.borrowObject(Event.Type.CALL_RETURN);
                initCallReturnEvent(event, processId, invokeId);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow CallReturnEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new CallReturnEvent(processId, invokeId);
    }

    public CallThrowsEvent borrowCallThrowsEvent(final int processId,
                                                 final int invokeId,
                                                 final String throwException) {
        if (isEnable) {
            try {
                final CallThrowsEvent event = (CallThrowsEvent) pool.borrowObject(Event.Type.CALL_THROWS);
                initCallThrowsEvent(event, processId, invokeId, throwException);
                return event;
            } catch (Exception cause) {
                logger.warn("EventPool borrow CallThrowsEvent[processId={};invokeId={};] failed.",
                        processId, invokeId, cause);
            }
        }
        return new CallThrowsEvent(processId, invokeId, throwException);
    }


    /**
     * 归还事件对象
     *
     * @param event 事件对象
     */
    public void returnEvent(Event event) {
        if (isEnable) {
            try {
                pool.returnObject(event.type, event);
            } catch (Exception cause) {
                logger.warn("EventPool return event={} failed.", event, cause);
            }
        }
    }


    private static final int ILLEGAL_PROCESS_ID = -1;
    private static final int ILLEGAL_INVOKE_ID = -1;

    private static final Unsafe unsafe;
    private static final long processIdFieldInInvokeEventOffset;
    private static final long invokeIdFieldInInvokeEventOffset;
    private static final long javaClassLoaderFieldInBeforeEventOffset;
    private static final long javaClassNameFieldInBeforeEventOffset;
    private static final long javaMethodNameFieldInBeforeEventOffset;
    private static final long javaMethodDescFieldInBeforeEventOffset;
    private static final long targetFieldInBeforeEventOffset;
    private static final long argumentArrayFieldInBeforeEventOffset;
    private static final long objectFieldInReturnEventOffset;
    private static final long throwableFieldInThrowsEventOffset;
    private static final long lineNumberFieldInLineEventOffset;

    private static final long lineNumberFieldInCallBeforeEventOffset;
    private static final long ownerFieldInCallBeforeEventOffset;
    private static final long nameFieldInCallBeforeEventOffset;
    private static final long descFieldInCallBeforeEventOffset;
    private static final long throwExceptionFieldInCallThrowsEventOffset;

    static {
        try {
            unsafe = UnsafeUtils.getUnsafe();
            processIdFieldInInvokeEventOffset = unsafe.objectFieldOffset(InvokeEvent.class.getDeclaredField("processId"));
            invokeIdFieldInInvokeEventOffset = unsafe.objectFieldOffset(InvokeEvent.class.getDeclaredField("invokeId"));
            javaClassLoaderFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaClassLoader"));
            javaClassNameFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaClassName"));
            javaMethodNameFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaMethodName"));
            javaMethodDescFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaMethodDesc"));
            targetFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("target"));
            argumentArrayFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("argumentArray"));
            objectFieldInReturnEventOffset = unsafe.objectFieldOffset(ReturnEvent.class.getDeclaredField("object"));
            throwableFieldInThrowsEventOffset = unsafe.objectFieldOffset(ThrowsEvent.class.getDeclaredField("throwable"));
            lineNumberFieldInLineEventOffset = unsafe.objectFieldOffset(LineEvent.class.getDeclaredField("lineNumber"));

            lineNumberFieldInCallBeforeEventOffset = unsafe.objectFieldOffset(CallBeforeEvent.class.getDeclaredField("lineNumber"));
            ownerFieldInCallBeforeEventOffset = unsafe.objectFieldOffset(CallBeforeEvent.class.getDeclaredField("owner"));
            nameFieldInCallBeforeEventOffset = unsafe.objectFieldOffset(CallBeforeEvent.class.getDeclaredField("name"));
            descFieldInCallBeforeEventOffset = unsafe.objectFieldOffset(CallBeforeEvent.class.getDeclaredField("desc"));
            throwExceptionFieldInCallThrowsEventOffset = unsafe.objectFieldOffset(CallThrowsEvent.class.getDeclaredField("throwException"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }


    private static void initBeforeEvent(final BeforeEvent event,
                                        final int processId,
                                        final int invokeId,
                                        final ClassLoader javaClassLoader,
                                        final String javaClassName,
                                        final String javaMethodName,
                                        final String javaMethodDesc,
                                        final Object target,
                                        final Object[] argumentArray) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(event, javaClassLoaderFieldInBeforeEventOffset, javaClassLoader);
        unsafe.putObject(event, javaClassNameFieldInBeforeEventOffset, javaClassName);
        unsafe.putObject(event, javaMethodNameFieldInBeforeEventOffset, javaMethodName);
        unsafe.putObject(event, javaMethodDescFieldInBeforeEventOffset, javaMethodDesc);
        unsafe.putObject(event, targetFieldInBeforeEventOffset, target);
        unsafe.putObject(event, argumentArrayFieldInBeforeEventOffset, argumentArray);
    }

    private static void initReturnEvent(final ReturnEvent event,
                                        final int processId,
                                        final int invokeId,
                                        final Object returnObj) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(event, objectFieldInReturnEventOffset, returnObj);
    }

    private static void initThrowsEvent(final ThrowsEvent event,
                                        final int processId,
                                        final int invokeId,
                                        final Throwable throwable) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(event, throwableFieldInThrowsEventOffset, throwable);
    }

    private static void initLineEvent(final LineEvent event,
                                      final int processId,
                                      final int invokeId,
                                      final int lineNumber) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putInt(event, lineNumberFieldInLineEventOffset, lineNumber);
    }

    private static void initCallBeforeEvent(final CallBeforeEvent event,
                                            final int processId,
                                            final int invokeId,
                                            final int lineNumber,
                                            final String owner,
                                            final String name,
                                            final String desc) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putInt(event, lineNumberFieldInCallBeforeEventOffset, lineNumber);
        unsafe.putObject(event, ownerFieldInCallBeforeEventOffset, owner);
        unsafe.putObject(event, nameFieldInCallBeforeEventOffset, name);
        unsafe.putObject(event, descFieldInCallBeforeEventOffset, desc);
    }

    private static void initCallReturnEvent(final CallReturnEvent event,
                                            final int processId,
                                            final int invokeId) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
    }

    private static void initCallThrowsEvent(final CallThrowsEvent event,
                                            final int processId,
                                            final int invokeId,
                                            final String throwException) {
        unsafe.putInt(event, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(event, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(event, throwExceptionFieldInCallThrowsEventOffset, throwException);
    }

    private static class EventFactory extends BaseKeyedPooledObjectFactory<Event.Type, Event> {

        @Override
        public Event create(Event.Type type) throws Exception {
            switch (type) {
                case BEFORE:
                    return new BeforeEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null, null, null, null, null, null);
                case THROWS:
                    return new ThrowsEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
                case RETURN:
                    return new ReturnEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
                case LINE:
                    return new LineEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, -1);
                case IMMEDIATELY_RETURN:
                    return new ImmediatelyReturnEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
                case IMMEDIATELY_THROWS:
                    return new ImmediatelyThrowsEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
                case CALL_BEFORE:
                    return new CallBeforeEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, -1, null, null, null);
                case CALL_RETURN:
                    return new CallReturnEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID);
                case CALL_THROWS:
                    return new CallThrowsEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
            }
            throw new IllegalStateException("illegal type=" + type);
        }

        @Override
        public PooledObject<Event> wrap(Event event) {
            return new DefaultPooledObject<Event>(event);
        }

        /*
         * 这里主要是释放掉引用的大资源，比如入参、返回值、抛异常等
         * 一些不大的资源其实可以保持引用，不会轻易触发GC
         */
        @Override
        public void passivateObject(Event.Type key, PooledObject<Event> pooledObject) throws Exception {
            final Event event = pooledObject.getObject();
            switch (event.type) {
                case BEFORE:
                    unsafe.putObject(event, targetFieldInBeforeEventOffset, null);
                    unsafe.putObject(event, argumentArrayFieldInBeforeEventOffset, null);
                    break;
                case IMMEDIATELY_THROWS:
                case THROWS:
                    unsafe.putObject(event, throwableFieldInThrowsEventOffset, null);
                    break;
                case IMMEDIATELY_RETURN:
                case RETURN:
                    unsafe.putObject(event, objectFieldInReturnEventOffset, null);
            }
        }

    }

}
