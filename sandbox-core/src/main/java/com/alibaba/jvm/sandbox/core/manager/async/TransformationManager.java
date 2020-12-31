package com.alibaba.jvm.sandbox.core.manager.async;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultModuleEventWatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/6/10 3:26 下午
 */
public class TransformationManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int MAX_QUEUE_SIZE = 512;

    private Disruptor<TransformationEvent> disruptor;

    private static TransformationManager instance;

    public static TransformationManager getInstance() {

        if (instance == null) {
            synchronized (TransformationManager.class) {
                if (instance == null) {
                    instance = new TransformationManager();
                }
            }
        }

        return instance;
    }

    private TransformationManager() {
        init();
    }

    private void init() {

        logger.info("On initializing transformation disruptor...");

        ThreadFactory threadFactory = new ThreadFactory() {

            private final AtomicInteger threadNumber = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Handling transformation event - " + threadNumber.getAndIncrement());
                if (t.isDaemon()) {
                    t.setDaemon(true);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }

        };

        EventFactory<TransformationEvent> factory = new EventFactory<TransformationEvent>() {
            @Override
            public TransformationEvent newInstance() {
                return new TransformationEvent();
            }
        };

        // park for 1 minute
        SleepingWaitStrategy sleepingWaitStrategy = new SleepingWaitStrategy(0, TimeUnit.MILLISECONDS.toNanos(1L));

        // only one thread could be used
        this.disruptor = new Disruptor<>(factory, MAX_QUEUE_SIZE, threadFactory, ProducerType.MULTI, sleepingWaitStrategy);

        TransformationEventHandler transformationEventHandler = new TransformationEventHandler();

        this.disruptor.handleEventsWith(transformationEventHandler)
                .thenHandleEventsWithWorkerPool(transformationEventHandler, transformationEventHandler, transformationEventHandler, transformationEventHandler);

        this.disruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler());

        this.disruptor.start();

        logger.info("Disruptor for transformation is running...");

    }

    public void onData(int watchId,
                       Matcher matcher,
                       EventListener listener,
                       ModuleEventWatcher.Progress progress,
                       Event.Type[] eventType,
                       DefaultModuleEventWatcher eventWatcher) {

        RingBuffer<TransformationEvent> ringBuffer = disruptor.getRingBuffer();

        long sequence = ringBuffer.next();

        try {
            TransformationEvent transformationEvent = ringBuffer.get(sequence);
            transformationEvent.setWatchId(watchId);
            transformationEvent.setMatcher(matcher);
            transformationEvent.setListener(listener);
            transformationEvent.setProgress(progress);
            transformationEvent.setEventTypes(eventType);
            transformationEvent.setDefaultModuleEventWatcher(eventWatcher);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void halt() {
        this.disruptor.halt();
    }

    public void shutdown() {
        this.disruptor.shutdown();
    }

    public Disruptor<TransformationEvent> getDisruptor() {
        return disruptor;
    }

}
