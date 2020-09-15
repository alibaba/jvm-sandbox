package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.util.GaStringUtils;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.TracingEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.NativeClass;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;

import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_BEFORE;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_RETURN;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_THROWS;

import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_SYSTEM;

/**
 * @author zhuangpeng
 * @since 2020/9/13
 */
public class NativeClassEnhanceTestCase {
    @Test
    public void call$currentTimeMillis() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                NATIVECLASS_SYSTEM,
                listener = new TracingEventListener(),
                CALL_BEFORE, CALL_RETURN, CALL_THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(System.class));
    }
}
