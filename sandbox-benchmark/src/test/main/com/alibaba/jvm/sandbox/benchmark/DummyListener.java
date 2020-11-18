package com.alibaba.jvm.sandbox.benchmark;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 4:06 下午
 */
public class DummyListener extends AdviceListener {

    @Override
    protected void before(Advice advice) throws Throwable {

        if (!advice.isProcessTop()) {
            return;
        }

        if (advice.getTarget() == null) {
            return;
        }

    }

    @Override
    protected void afterReturning(Advice advice) throws Throwable {

    }

}
