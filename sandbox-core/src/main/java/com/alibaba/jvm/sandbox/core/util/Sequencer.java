package com.alibaba.jvm.sandbox.core.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 序列发生器
 * 序列发生器用途非常广泛,主要用于圈定全局唯一性标识
 * Created by luanjia@taobao.com on 16/5/20.
 */
public class Sequencer {

    // 序列生成器
    private final AtomicInteger generator;

    public Sequencer(int initialValue) {
        generator = new AtomicInteger(initialValue);
    }

    /**
     * 生成下一条序列
     *
     * @return 下一条序列
     */
    public int next() {
        return generator.getAndIncrement();
    }

}
