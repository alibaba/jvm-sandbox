package com.alibaba.jvm.sandbox.benchmark;

import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.rule.SandboxRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Average: 782 ms
 *
 * TODO benchmark with jmh
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 9:43 下午
 */
public class BenchmarkForParallelWatching {

    @Rule
    public SandboxRule sandboxRule = new SandboxRule();

    static {
        Holder.withLazyReload = true;
    }

    @Test
    @PrepareForTest(testModule = DummyModule.class, fastMode = false)
    public void sync_parallel_watching_benchmark() {

    }

}
