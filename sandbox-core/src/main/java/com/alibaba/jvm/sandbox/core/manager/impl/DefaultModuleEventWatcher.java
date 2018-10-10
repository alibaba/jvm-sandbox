package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus;
import com.alibaba.jvm.sandbox.core.util.Sequencer;
import com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.GroupMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static com.alibaba.jvm.sandbox.api.filter.ExtFilter.ExtFilterFactory.make;
import static com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher.toOrGroupMatcher;

/**
 * 默认事件观察者实现
 * Created by luanjia@taobao.com on 2017/2/23.
 */
public class DefaultModuleEventWatcher implements ModuleEventWatcher, ModuleLifeCycleEventBus.ModuleLifeCycleEventListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Instrumentation inst;
    private final CoreLoadedClassDataSource classDataSource;
    private final CoreModule coreModule;
    private final boolean isEnableUnsafe;

    // 观察ID序列生成器
    private final Sequencer watchIdSequencer = new Sequencer(1000);

    DefaultModuleEventWatcher(final Instrumentation inst,
                              final CoreLoadedClassDataSource classDataSource,
                              final CoreModule coreModule,
                              final boolean isEnableUnsafe) {
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.coreModule = coreModule;
        this.isEnableUnsafe = isEnableUnsafe;
    }


    // 开始进度
    private void beginProgress(final Progress progress,
                               final int total) {
        if (null != progress) {
            try {
                progress.begin(total);
            } catch (Throwable cause) {
                logger.warn("begin progress failed.", cause);
            }
        }
    }

    // 结束进度
    private void finishProgress(final Progress progress, final int cCnt, final int mCnt) {
        if (null != progress) {
            try {
                progress.finish(cCnt, mCnt);
            } catch (Throwable cause) {
                logger.warn("finish progress failed.", cause);
            }
        }
    }

    /*
     * 形变观察所影响的类
     */
    private void reTransformClasses(final int watchId,
                                    final List<Class<?>> waitingReTransformClasses,
                                    final Progress progress) {

        // 需要形变总数
        final int total = waitingReTransformClasses.size();

        // 如果找不到需要被重新增强的类则直接返回
        if (CollectionUtils.isEmpty(waitingReTransformClasses)) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("reTransformClasses:{};module[id:{}];watch[Id:{}];",
                    waitingReTransformClasses, coreModule.getUniqueId(), watchId);
        }


        // 如果不需要进行进度汇报,则可以进行批量形变
        boolean batchReTransformSuccess = true;
        if (null == progress) {
            try {
                inst.retransformClasses(waitingReTransformClasses.toArray(new Class[waitingReTransformClasses.size()]));
                logger.info("module[id:{}] watch[id:{}] batch reTransform classes[count:{}] success.",
                        coreModule.getUniqueId(), watchId, waitingReTransformClasses.size());
            } catch (Throwable e) {
                logger.warn("module[id:{}] watch[id:{}] batch reTransform classes[count:{}] failed.",
                        coreModule.getUniqueId(), watchId, waitingReTransformClasses.size(), e);
                batchReTransformSuccess = false;
            }
        }

        // 只有两种情况需要进行逐个形变
        // 1. 需要进行形变进度报告,则只能一个个进行形变
        // 2. 批量形变失败,需要转换为单个形变,以观察具体是哪个形变失败
        if (!batchReTransformSuccess
                || null != progress) {
            int index = 0;
            for (final Class<?> waitingReTransformClass : waitingReTransformClasses) {
                index++;
                try {
                    if (null != progress) {
                        try {
                            progress.progressOnSuccess(waitingReTransformClass, index);
                        } catch (Throwable cause) {
                            // 在进行进度汇报的过程中抛出异常,直接进行忽略,因为不影响形变的主体流程
                            // 仅仅只是一个汇报作用而已
                            logger.warn("module[id:{}] watch[id:{}] on class:{} report progressOnSuccess occur exception at index:{},total:{},class:{};",
                                    coreModule.getUniqueId(), watchId, waitingReTransformClass,
                                    index - 1, total,
                                    cause
                            );
                        }
                    }
                    inst.retransformClasses(waitingReTransformClass);
                    logger.info("module[id:{}] watch[id:{}] single reTransform class:{} success, at index:{},total:{};",
                            coreModule.getUniqueId(), watchId, waitingReTransformClass,
                            index - 1, total
                    );
                } catch (Throwable causeOfReTransform) {
                    logger.warn("module[id:{}] watch[id:{}] single reTransform class:{} failed, at index:{},total:{}. ignore this class.",
                            coreModule.getUniqueId(), watchId, waitingReTransformClass,
                            index - 1, total,
                            causeOfReTransform
                    );
                    if (null != progress) {
                        try {
                            progress.progressOnFailed(waitingReTransformClass, index, causeOfReTransform);
                        } catch (Throwable cause) {
                            logger.warn("module[id:{}] watch[id:{}] on class:{} report progressOnFailed occur exception, at index:{},total:{};",
                                    coreModule.getUniqueId(), watchId, waitingReTransformClass,
                                    index - 1, total,
                                    cause
                            );
                        }
                    }
                }
            }//for
        }

    }

    @Override
    public int watch(final Filter filter,
                     final EventListener listener,
                     final Event.Type... eventType) {
        return watch(filter, listener, null, eventType);
    }

    @Override
    public int watch(final Filter filter,
                     final EventListener listener,
                     final Progress progress,
                     final Event.Type... eventType) {
        return watch(new ExtFilterMatcher(make(filter)), listener, progress, eventType);
    }

    @Override
    public int watch(final EventWatchCondition condition,
                     final EventListener listener,
                     final Progress progress,
                     final Event.Type... eventType) {
        return watch(toOrGroupMatcher(condition.getOrFilterArray()), listener, progress, eventType);
    }

    // 这里是用matcher重制过后的watch
    private int watch(final Matcher matcher,
                      final EventListener listener,
                      final Progress progress,
                      final Event.Type... eventType) {
        final int watchId = watchIdSequencer.next();
        // 给对应的模块追加ClassFileTransformer
        final SandboxClassFileTransformer sandClassFileTransformer = new SandboxClassFileTransformer(
                watchId, coreModule.getUniqueId(), matcher, listener, isEnableUnsafe, eventType);

        // 注册到CoreModule中
        coreModule.getSandboxClassFileTransformers().add(sandClassFileTransformer);

        // 注册到JVM加载上ClassFileTransformer处理新增的类
        inst.addTransformer(sandClassFileTransformer, true);

        // 查找需要渲染的类集合
        final List<Class<?>> waitingReTransformClasses = classDataSource.findForReTransform(matcher);
        logger.info("{} watch[id:{}] found classes:{} in loaded for watch(ing).",
                coreModule, watchId, waitingReTransformClasses.size());

        int cCnt = 0, mCnt = 0;

        // 进度通知启动
        beginProgress(progress, waitingReTransformClasses.size());
        try {

            // 应用JVM
            reTransformClasses(watchId, waitingReTransformClasses, progress);

            // 计数
            cCnt += sandClassFileTransformer.getAffectStatistic().cCnt();
            mCnt += sandClassFileTransformer.getAffectStatistic().mCnt();


            // 激活增强类
            if (coreModule.isActivated()) {
                final int listenerId = sandClassFileTransformer.getListenerId();
                EventListenerHandlers.getSingleton()
                        .active(listenerId, listener, eventType);
            }

        } finally {
            finishProgress(progress, cCnt, mCnt);
        }

        return watchId;
    }

    @Override
    public void delete(final int watcherId,
                       final Progress progress) {

        final Set<Matcher> waitingRemoveMatcherSet = new LinkedHashSet<Matcher>();

        // 找出待删除的SandboxClassFileTransformer
        final Iterator<SandboxClassFileTransformer> cftIt = coreModule.getSandboxClassFileTransformers().iterator();
        int cCnt = 0, mCnt = 0;
        while (cftIt.hasNext()) {
            final SandboxClassFileTransformer sandboxClassFileTransformer = cftIt.next();
            if (watcherId == sandboxClassFileTransformer.getWatchId()) {

                // 冻结所有关联代码增强
                EventListenerHandlers.getSingleton()
                        .frozen(sandboxClassFileTransformer.getListenerId());

                // 在JVM中移除掉命中的ClassFileTransformer
                inst.removeTransformer(sandboxClassFileTransformer);

                // 计数
                cCnt += sandboxClassFileTransformer.getAffectStatistic().cCnt();
                mCnt += sandboxClassFileTransformer.getAffectStatistic().mCnt();

                // 追加到待删除过滤器集合
                waitingRemoveMatcherSet.add(sandboxClassFileTransformer.getMatcher());

                // 清除掉该SandboxClassFileTransformer
                cftIt.remove();

            }
        }

        // 查找需要删除后重新渲染的类集合
        final List<Class<?>> waitingReTransformClasses = classDataSource.findForReTransform(
                new GroupMatcher.Or(waitingRemoveMatcherSet.toArray(new Matcher[0]))
        );
        logger.info("{} found classes:{} in loaded for delete.",
                coreModule, waitingReTransformClasses.size());

        beginProgress(progress, waitingReTransformClasses.size());
        try {
            // 应用JVM
            reTransformClasses(watcherId, waitingReTransformClasses, progress);
        } finally {
            finishProgress(progress, cCnt, mCnt);
        }
    }

    @Override
    public void delete(int watcherId) {
        delete(watcherId, null);
    }

    @Override
    public void watching(Filter filter, EventListener listener, WatchCallback watchCb, Event.Type... eventType) throws Throwable {
        watching(filter, listener, null, watchCb, null, eventType);
    }

    @Override
    public void watching(final Filter filter,
                         final EventListener listener,
                         final Progress wProgress,
                         final WatchCallback watchCb,
                         final Progress dProgress,
                         final Event.Type... eventType) throws Throwable {
        final int watchId = watch(filter, listener, wProgress, eventType);
        try {
            watchCb.watchCompleted();
        } finally {
            delete(watchId, dProgress);
        }
    }

    @Override
    public boolean onFire(final CoreModule coreModule,
                          final ModuleLifeCycleEventBus.Event event) {

        if (this.coreModule == coreModule
                && event == ModuleLifeCycleEventBus.Event.UNLOAD) {
            // 是当前模块的卸载事件，需要主动清理掉之前的埋点
            for (final SandboxClassFileTransformer transformer : new ArrayList<SandboxClassFileTransformer>(coreModule.getSandboxClassFileTransformers())) {
                logger.info("delete watch[id={}] by module[id={};] unload.",
                        transformer.getWatchId(), coreModule.getUniqueId());
                delete(transformer.getWatchId());
            }
            // 当前模块都卸载了，我还留着做啥...
            return false;
        }

        // 不是当前模块的卸载事件，继续保持监听
        return true;
    }
}
