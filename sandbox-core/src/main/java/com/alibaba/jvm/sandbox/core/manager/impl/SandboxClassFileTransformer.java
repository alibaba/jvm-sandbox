package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.enhance.Enhancer;
import com.alibaba.jvm.sandbox.core.enhance.EventEnhancer;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * 沙箱类形变器
 * Created by luanjia@taobao.com on 2016/11/14.
 */
public class SandboxClassFileTransformer implements ClassFileTransformer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int watchId;
    private final String uniqueId;
    private final int listenerId;
    private final Filter filter;
    private final EventListener eventListener;
    private final boolean isEnableUnsafe;
    private final Event.Type[] eventTypeArray;

    // 影响类去重码集合
    private final Set<String> affectClassUniqueSet = new HashSet<String>();

    // 影响方法去重码集合
    private final Set<String> affectMethodUniqueSet = new HashSet<String>();


    SandboxClassFileTransformer(final int watchId,
                                final String uniqueId,
                                final Filter filter,
                                final EventListener eventListener,
                                final boolean isEnableUnsafe,
                                final Event.Type[] eventTypeArray) {
        this.watchId = watchId;
        this.uniqueId = uniqueId;
        this.listenerId = ObjectIDs.instance.identity(eventListener);
        this.filter = filter;
        this.eventListener = eventListener;
        this.isEnableUnsafe = isEnableUnsafe;
        this.eventTypeArray = eventTypeArray;
    }

    // 计算唯一编码前缀
    private String computeUniqueCodePrefix(final ClassLoader loader, final String javaClassName) {
        final StringBuilder codeSB = new StringBuilder();
        return codeSB.append(ObjectIDs.instance.identity(loader)).append("_").append(javaClassName).toString();

    }

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String javaClassName,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] srcByteCodeArray) throws IllegalClassFormatException {

        final String uniqueCodePrefix = computeUniqueCodePrefix(loader, javaClassName);

        final Enhancer enhancer = new EventEnhancer(
                listenerId,
                filter,
                uniqueCodePrefix,
                affectMethodUniqueSet,
                isEnableUnsafe,
                eventTypeArray
        );
        try {
            final byte[] toByteCodeArray = enhancer.toByteCodeArray(loader, srcByteCodeArray);
            if (srcByteCodeArray == toByteCodeArray) {
                logger.debug("enhancer ignore this class={}", javaClassName);
                return null;
            }

            // affect count
            affectClassUniqueSet.add(uniqueCodePrefix);

            logger.info("enhancer toByteCode success, module[id={}];class={};loader={};", uniqueId, javaClassName, loader);
            return toByteCodeArray;
        } catch (Throwable cause) {
            logger.warn("enhancer toByteCode failed, module[id={}];class={};loader={};", uniqueId, javaClassName, loader, cause);
            // throw new IllegalClassFormatException(cause.getMessage());
            return null;
        }
    }

    /**
     * 获取观察ID
     *
     * @return 观察ID
     */
    int getWatchId() {
        return watchId;
    }

    /**
     * 获取事件监听器
     *
     * @return 事件监听器
     */
    EventListener getEventListener() {
        return eventListener;
    }

    /**
     * 获取事件监听器ID
     *
     * @return 事件监听器ID
     */
    int getListenerId() {
        return listenerId;
    }

    /**
     * 获取类和方法过滤器
     *
     * @return 类和方法过滤器
     */
    Filter getFilter() {
        return filter;
    }

    /**
     * 获取本次监听事件类型数组
     *
     * @return 本次监听事件类型数组
     */
    Event.Type[] getEventTypeArray() {
        return eventTypeArray;
    }

    /**
     * 获取影响类数量
     *
     * @return 影响类数量
     */
    public int cCnt() {
        return affectClassUniqueSet.size();
    }

    /**
     * 获取影响方法数量
     *
     * @return 影响方法数量
     */
    public int mCnt() {
        return affectMethodUniqueSet.size();
    }

}
