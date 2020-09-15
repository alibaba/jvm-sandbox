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

import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM1;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM2;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM3;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM4;
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

    @Test
    public void call$param1() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                NATIVECLASS_PARAM1,
                listener = new TracingEventListener(),
                CALL_BEFORE, CALL_RETURN, CALL_THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(System.class));
    }

    @Test
    public void call$param2() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                NATIVECLASS_PARAM2,
                listener = new TracingEventListener(),
                CALL_BEFORE, CALL_RETURN, CALL_THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(System.class));
    }

    @Test
    public void call$param3() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                NATIVECLASS_PARAM3,
                listener = new TracingEventListener(),
                CALL_BEFORE, CALL_RETURN, CALL_THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(System.class));
    }

    @Test
    public void call$param4() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                NATIVECLASS_PARAM4,
                listener = new TracingEventListener(),
                CALL_BEFORE, CALL_RETURN, CALL_THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(System.class));
    }
}
