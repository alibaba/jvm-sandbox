package com.alibaba.jvm.sandbox.api.listener.ext;

import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.util.BehaviorDescriptor;
import com.alibaba.jvm.sandbox.api.util.CacheGet;
import com.alibaba.jvm.sandbox.api.util.GaStringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Stack;

/**
 * 通知监听器
 *
 * @author luanjia@taobao.com
 * @since {@code sandbox-api:1.0.10}
 */
class AdviceAdapterListener implements EventListener {

    private final AdviceListener adviceListener;

    AdviceAdapterListener(final AdviceListener adviceListener) {
        this.adviceListener = adviceListener;
    }

    private final ThreadLocal<OpStack> opStackRef = new ThreadLocal<OpStack>() {
        @Override
        protected OpStack initialValue() {
            return new OpStack();
        }
    };

    @Override
    final public void onEvent(final Event event) throws Throwable {
        switch (event.type) {
            case BEFORE: {
                final BeforeEvent bEvent = (BeforeEvent) event;
                final ClassLoader loader = toClassLoader(bEvent.javaClassLoader);
                final Class<?> targetClass = toClass(loader, bEvent.javaClassName);
                final Advice advice = new Advice(
                        bEvent.processId,
                        bEvent.invokeId,
                        toBehavior(
                                targetClass,
                                bEvent.javaMethodName,
                                bEvent.javaMethodDesc
                        ),
                        bEvent.argumentArray,
                        bEvent.target
                );

                final OpStack opStack = opStackRef.get();
                final Advice top;
                final Advice parent;

                // 顶层调用
                if (opStack.isEmpty()) {
                    top = parent = advice;
                }

                // 非顶层
                else {
                    parent = opStack.peek().advice;
                    top = parent.getProcessTop();
                }

                advice.applyBefore(top, parent);

                opStackRef.get().pushForBegin(advice);
                adviceListener.before(advice);
                break;
            }
            case RETURN: {
                final OpStack opStack = opStackRef.get();
                final ReturnEvent rEvent = (ReturnEvent) event;
                final WrapAdvice wrapAdvice = opStack.popByExpectInvokeId(rEvent.invokeId);
                if (null != wrapAdvice) {
                    adviceListener.afterReturning(wrapAdvice.advice.applyReturn(rEvent.object));
                }
                break;
            }
            case THROWS: {
                final OpStack opStack = opStackRef.get();
                final ThrowsEvent tEvent = (ThrowsEvent) event;
                final WrapAdvice wrapAdvice = opStack.popByExpectInvokeId(tEvent.invokeId);
                if (null != wrapAdvice) {
                    adviceListener.afterThrowing(wrapAdvice.advice.applyThrows(tEvent.throwable));
                }
                break;
            }

            case CALL_BEFORE: {
                final OpStack opStack = opStackRef.get();
                final CallBeforeEvent cbEvent = (CallBeforeEvent) event;
                final WrapAdvice wrapAdvice = opStack.peekByExpectInvokeId(cbEvent.invokeId);
                if (null == wrapAdvice) {
                    return;
                }
                final CallTarget target;
                wrapAdvice.attach(target = new CallTarget(
                        cbEvent.lineNumber,
                        toJavaClassName(cbEvent.owner),
                        cbEvent.name,
                        cbEvent.desc
                ));
                adviceListener.beforeCall(
                        wrapAdvice.advice,
                        target.callLineNum,
                        target.callJavaClassName,
                        target.callJavaMethodName,
                        target.callJavaMethodDesc
                );
                break;
            }

            case CALL_RETURN: {
                final OpStack opStack = opStackRef.get();
                final CallReturnEvent crEvent = (CallReturnEvent) event;
                final WrapAdvice wrapAdvice = opStack.peekByExpectInvokeId(crEvent.invokeId);
                if (null == wrapAdvice) {
                    return;
                }
                final CallTarget target = wrapAdvice.attachment();
                if (null == target) {
                    // 这里做一个容灾保护，防止在callBefore()中发生什么异常导致beforeCall()之前失败
                    return;
                }
                adviceListener.afterCallReturning(
                        wrapAdvice.advice,
                        target.callLineNum,
                        target.callJavaClassName,
                        target.callJavaMethodName,
                        target.callJavaMethodDesc
                );
                break;
            }

            case CALL_THROWS: {
                final OpStack opStack = opStackRef.get();
                final CallThrowsEvent ctEvent = (CallThrowsEvent) event;
                final WrapAdvice wrapAdvice = opStack.peekByExpectInvokeId(ctEvent.invokeId);
                if (null == wrapAdvice) {
                    return;
                }
                final CallTarget target = wrapAdvice.attachment();
                if (null == target) {
                    // 这里做一个容灾保护，防止在callBefore()中发生什么异常导致beforeCall()之前失败
                    return;
                }
                adviceListener.afterCallThrowing(
                        wrapAdvice.advice,
                        target.callLineNum,
                        target.callJavaClassName,
                        target.callJavaMethodName,
                        target.callJavaMethodDesc,
                        ctEvent.throwException
                );
                break;
            }

            case LINE: {
                final OpStack opStack = opStackRef.get();
                final LineEvent lEvent = (LineEvent) event;
                final WrapAdvice wrapAdvice = opStack.peekByExpectInvokeId(lEvent.invokeId);
                if (null == wrapAdvice) {
                    return;
                }
                adviceListener.beforeLine(wrapAdvice.advice, lEvent.lineNumber);
                break;
            }

            default:
                //ignore
        }//switch
    }


    // --- 以下为内部操作实现 ---


    /**
     * 通知操作堆栈
     */
    private class OpStack {

        private final Stack<WrapAdvice> adviceStack = new Stack<WrapAdvice>();

        boolean isEmpty() {
            return adviceStack.isEmpty();
        }

        WrapAdvice peek() {
            return adviceStack.peek();
        }

        void pushForBegin(final Advice advice) {
            adviceStack.push(new WrapAdvice(advice));
        }

        /**
         * 在通知堆栈中，BEFORE:[RETURN/THROWS]的invokeId是配对的，
         * 如果发生错位则说明BEFORE的事件没有被成功压入堆栈，没有被正确的处理，外界没有正确感知BEFORE
         * 所以这里也要进行修正行的忽略对应的[RETURN/THROWS]
         *
         * @param expectInvokeId 期待的invokeId
         *                       必须要求和BEFORE的invokeId配对
         * @return 如果invokeId配对成功，则返回对应的Advice，否则返回null
         */
        WrapAdvice popByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().advice.getInvokeId() == expectInvokeId
                    ? adviceStack.pop()
                    : null;
        }

        WrapAdvice peekByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().advice.getInvokeId() == expectInvokeId
                    ? adviceStack.peek()
                    : null;
        }

    }

    // change internalClassName to javaClassName
    private String toJavaClassName(final String internalClassName) {
        if (GaStringUtils.isEmpty(internalClassName)) {
            return internalClassName;
        } else {
            return internalClassName.replaceAll("/", ".");
        }
    }

    // 提取ClassLoader，从BeforeEvent中获取到的ClassLoader
    private ClassLoader toClassLoader(ClassLoader loader) {
        return null == loader
                // 如果此处为null，则说明遇到了来自Bootstrap的类，
                ? AdviceAdapterListener.class.getClassLoader()
                : loader;
    }

    // 根据JavaClassName从ClassLoader中提取出Class<?>对象
    private Class<?> toClass(ClassLoader loader, String javaClassName) throws ClassNotFoundException {
        return toClassLoader(loader).loadClass(javaClassName);
    }


    /**
     * 行为缓存KEY对象
     */
    private class BehaviorCacheKey {
        private final Class<?> clazz;
        private final String javaMethodName;
        private final String javaMethodDesc;

        private BehaviorCacheKey(final Class<?> clazz,
                                 final String javaMethodName,
                                 final String javaMethodDesc) {
            this.clazz = clazz;
            this.javaMethodName = javaMethodName;
            this.javaMethodDesc = javaMethodDesc;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode()
                    + javaMethodName.hashCode()
                    + javaMethodDesc.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (null == o
                    || !(o instanceof BehaviorCacheKey)) {
                return false;
            }
            final BehaviorCacheKey key = (BehaviorCacheKey) o;
            return clazz.equals(key.clazz)
                    && javaMethodName.equals(key.javaMethodName)
                    && javaMethodDesc.equals(key.javaMethodDesc);
        }

    }

    // 行为缓存，为了增加性能，不要每次都从class通过反射获取行为
    private final CacheGet<BehaviorCacheKey, Behavior> toBehaviorCacheGet
            = new CacheGet<BehaviorCacheKey, Behavior>() {
        @Override
        protected Behavior load(BehaviorCacheKey key) {
            if ("<init>".equals(key.javaMethodName)) {
                for (final Constructor<?> constructor : key.clazz.getDeclaredConstructors()) {
                    if (key.javaMethodDesc.equals(new BehaviorDescriptor(constructor).getDescriptor())) {
                        return new Behavior.ConstructorImpl(constructor);
                    }
                }
            } else {
                for (final Method method : key.clazz.getDeclaredMethods()) {
                    if (key.javaMethodName.equals(method.getName())
                            && key.javaMethodDesc.equals(new BehaviorDescriptor(method).getDescriptor())) {
                        return new Behavior.MethodImpl(method);
                    }
                }
            }
            return null;
        }
    };

    /**
     * CALL目标对象
     */
    private class CallTarget {

        final int callLineNum;
        final String callJavaClassName;
        final String callJavaMethodName;
        final String callJavaMethodDesc;

        CallTarget(int callLineNum, String callJavaClassName, String callJavaMethodName, String callJavaMethodDesc) {
            this.callLineNum = callLineNum;
            this.callJavaClassName = callJavaClassName;
            this.callJavaMethodName = callJavaMethodName;
            this.callJavaMethodDesc = callJavaMethodDesc;
        }
    }

    /**
     * 通知内部封装，主要是要封装掉attachment
     */
    private class WrapAdvice implements Attachment {

        final Advice advice;
        Object attachment;

        WrapAdvice(Advice advice) {
            this.advice = advice;
        }

        @Override
        public void attach(Object attachment) {
            this.attachment = attachment;
        }

        @Override
        public <T> T attachment() {
            return (T) attachment;
        }
    }

    /**
     * 根据提供的行为名称、行为描述从指定的Class中获取对应的行为
     *
     * @param clazz          指定的Class
     * @param javaMethodName 行为名称
     * @param javaMethodDesc 行为参数声明
     * @return 匹配的行为
     * @throws NoSuchMethodException 如果匹配不到行为，则抛出该异常
     */
    private Behavior toBehavior(final Class<?> clazz,
                                final String javaMethodName,
                                final String javaMethodDesc) throws NoSuchMethodException {
        final Behavior behavior = toBehaviorCacheGet.getFromCache(new BehaviorCacheKey(clazz, javaMethodName, javaMethodDesc));
        if (null == behavior) {
            throw new NoSuchMethodException(String.format("%s.%s(%s)", clazz.getName(), javaMethodName, javaMethodDesc));
        }
        return behavior;
    }

}
