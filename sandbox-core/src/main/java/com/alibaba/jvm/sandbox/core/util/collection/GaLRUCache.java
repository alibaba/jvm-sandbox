package com.alibaba.jvm.sandbox.core.util.collection;

import java.util.LinkedHashMap;
import java.util.Map;

public class GaLRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxCapacity;

    public GaLRUCache(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }

}
