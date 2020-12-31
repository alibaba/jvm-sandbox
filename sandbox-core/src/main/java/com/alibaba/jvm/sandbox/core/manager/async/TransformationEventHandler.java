package com.alibaba.jvm.sandbox.core.manager.async;

import com.alibaba.jvm.sandbox.api.filter.ClassIdentifiable;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultModuleEventWatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;
import com.alibaba.jvm.sandbox.core.util.matcher.inner.BehaviorMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Handling {@link TransformationEvent} than report
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/6/10 11:35 上午
 */
public class TransformationEventHandler implements EventHandler<TransformationEvent>, WorkHandler<TransformationEvent> {

    private static Map<String, List<TransformationEvent>> transformationEventMap = Maps.newLinkedHashMap();

    private static final Queue<Map<String, List<TransformationEvent>>> QUEUE = new ArrayBlockingQueue<>(300);

    @Override
    public void onEvent(TransformationEvent event, long sequence, boolean endOfBatch) throws Exception {

        offer(event);

        // keep balance
//        if (transformationEventMap.size() >= 2) {
//            Map<String, List<TransformationEvent>> transformationEvents = take(20);
//            QUEUE.offer(transformationEvents);
//        }

        if (endOfBatch) {
            Map<String, List<TransformationEvent>> transformationEvents = takeAll();
            QUEUE.offer(transformationEvents);
        }


    }

    @Override
    public void onEvent(TransformationEvent event) throws Exception {

        // if it is the end of batch, compute all of them
        Map<String, List<TransformationEvent>> transformationEvents = QUEUE.poll();

        if (transformationEvents == null || event.getDefaultModuleEventWatcher() == null) {
            return;
        }

        DefaultModuleEventWatcher eventWatcher = event.getDefaultModuleEventWatcher();

        for (Map.Entry<String, List<TransformationEvent>> entry : transformationEvents.entrySet()) {
            List<TransformationEvent> transformationEventList = entry.getValue();
            Matcher matcher = transformationEventList.get(0).getMatcher();

            // if it is a ExtFilterMatcher, just merge them
            if (matcher instanceof ExtFilterMatcher) {
                ExtFilterMatcher theMatcher = (ExtFilterMatcher) matcher;
                List<BehaviorMatcher> behaviorMatchers = Lists.newArrayList();
                for (TransformationEvent transformationEvent : transformationEventList) {
                    behaviorMatchers.addAll(((ExtFilterMatcher) transformationEvent.getMatcher()).getBehaviorMatchers());
                }
                ExtFilterMatcher extFilterMatcher = new ExtFilterMatcher(theMatcher, behaviorMatchers);
                eventWatcher.batchTransform(extFilterMatcher, transformationEventList);
            } else {// else just transform one by one
                for (TransformationEvent transformationEvent : transformationEventList) {
                    eventWatcher.batchTransform(transformationEvent.getMatcher(), transformationEventList);
                }
            }
        }

    }


    private boolean offer(TransformationEvent transformationEvent) {

        if (transformationEvent == null) {
            return false;
        }

        Matcher matcher = transformationEvent.getMatcher();

        if (matcher == null) {
            return false;
        }

        if (!(matcher instanceof ClassIdentifiable)) {
            offer(RandomStringUtils.random(10, true, true), transformationEvent);
            return true;
        }

        String classIdentity = ((ClassIdentifiable) matcher).getClassIdentity();

        if (classIdentity == null) {
            return false;
        }

        offer(classIdentity, transformationEvent);

        return true;
    }

    private void offer(String classIdentity, TransformationEvent transformationEvent) {
        // put if absent
        if (!transformationEventMap.containsKey(classIdentity)) {
            transformationEventMap.put(classIdentity, Lists.newArrayList(transformationEvent));
        } else {
            transformationEventMap.get(classIdentity).add(transformationEvent);
        }
    }

    private Map<String, List<TransformationEvent>> take(int n) {

        if (transformationEventMap.size() <= n) {
            return takeAll();
        }

        Map<String, List<TransformationEvent>> copy = Maps.newHashMap();
        Iterator<Map.Entry<String, List<TransformationEvent>>> iterator = transformationEventMap.entrySet().iterator();

        int count = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, List<TransformationEvent>> entry = iterator.next();
            copy.put(entry.getKey(), entry.getValue());
            iterator.remove();
            if (++count >= n) {
                break;
            }
        }

        return copy;

    }

    private synchronized Map<String, List<TransformationEvent>> takeAll() {

        Map<String, List<TransformationEvent>> copy = new HashMap<>(transformationEventMap);

        transformationEventMap.clear();

        return copy;

    }


}
