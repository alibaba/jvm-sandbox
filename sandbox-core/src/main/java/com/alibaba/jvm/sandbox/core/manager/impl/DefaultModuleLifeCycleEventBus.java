package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 默认模块生命周期实现
 * Created by luanjia@taobao.com on 2017/2/3.
 */
public class DefaultModuleLifeCycleEventBus implements ModuleLifeCycleEventBus {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LinkedList<ModuleLifeCycleEventListener> moduleLifeCycleEventListeners
            = new LinkedList<ModuleLifeCycleEventListener>();

    @Override
    public void append(ModuleLifeCycleEventListener lifeCycleEventListener) {
        moduleLifeCycleEventListeners.add(lifeCycleEventListener);
    }

    @Override
    public void fire(CoreModule coreModule, Event event) {

        logger.info("firing module-event: event={};module={};", event, coreModule.getUniqueId());
        final Iterator<ModuleLifeCycleEventListener> listenerIt = moduleLifeCycleEventListeners.iterator();
        while (listenerIt.hasNext()) {

            final ModuleLifeCycleEventListener moduleLifeCycleEventListener = listenerIt.next();
            try {
                if (!moduleLifeCycleEventListener.onFire(coreModule, event)) {
                    // 监听器返回FALSE，说明监听器主动放弃后续的消息监听
                    listenerIt.remove();
                    logger.debug("fired module-event by once. event={};module={};listener={};",
                            event,
                            coreModule.getUniqueId(),
                            moduleLifeCycleEventListener
                    );
                }
            } catch (Throwable cause) {
                logger.warn("fire module-event failed, event={};module={};listener={};",
                        event,
                        coreModule.getUniqueId(),
                        moduleLifeCycleEventListener,
                        cause
                );
            }

        }

    }

}
