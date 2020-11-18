package com.alibaba.jvm.sandbox.benchmark;

import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.rule.SandboxRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Average: 2799 ms
 *
 * TODO benchmark with jmh
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 9:43 下午
 */
public class BenchmarkForWatching {

    @Rule
    public SandboxRule sandboxRule = new SandboxRule();

    static {
        Holder.withLazyReload = false;
    }

    @Test
    @PrepareForTest(testModule = DummyModule.class, fastMode = false)
    public void sync_watching_benchmark() {

    }

}
