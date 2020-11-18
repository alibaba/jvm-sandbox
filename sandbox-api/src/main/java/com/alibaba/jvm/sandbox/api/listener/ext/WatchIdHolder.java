package com.alibaba.jvm.sandbox.api.listener.ext;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@link WatchIdHolder} for lazy init.
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/16 10:37 上午
 */
public abstract class WatchIdHolder {

    private final static Set<Integer> finishedWatchIds = new ConcurrentSkipListSet<Integer>();

    private final static Set<Integer> waitingWatchIds = new ConcurrentSkipListSet<Integer>();

    public static void addWaitingWatchId(Integer watchId) {
        waitingWatchIds.add(watchId);
    }

    public static void addWaitingWatchIds(Collection<Integer> watchIds) {
        waitingWatchIds.addAll(watchIds);
    }

    public static void finish(Integer watchId) {
        if (waitingWatchIds.remove(watchId)) {
            finishedWatchIds.add(watchId);
        }
    }

    public static boolean hasUnfinishedWatchId() {
        return !waitingWatchIds.isEmpty();
    }

    public static Set<Integer> getWaitingWatchIds() {
        return waitingWatchIds;
    }

    public static Set<Integer> getFinishedWatchIds() {
        return finishedWatchIds;
    }

}
