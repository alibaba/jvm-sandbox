package com.alibaba.jvm.sandbox.qatest.core.enhance;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.Event.Type;
import com.alibaba.jvm.sandbox.api.util.GaStringUtils;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.TracingEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.NativeClass;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper.Transformer;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.BEFORE;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_BEFORE;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_RETURN;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_THROWS;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.RETURN;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.THROWS;
import static com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator.ERROR_EXCEPTION_MESSAGE;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM1;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM2;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM3;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_PARAM4;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.NATIVECLASS_SYSTEM;
import static org.junit.Assert.assertEquals;

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
                BEFORE, RETURN, THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));
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
                BEFORE, RETURN, THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));
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
                BEFORE, RETURN, THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));
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
                BEFORE, RETURN, THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));
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
                BEFORE, RETURN, THROWS
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));

        Object nativeClass = calculatorClass.newInstance();
        try {
            MethodUtils.invokeMethod(nativeClass,"param4","test","test");
        } catch (InvocationTargetException e) {
            assertEquals(e.getTargetException().getClass(), UnsatisfiedLinkError.class);
        } catch (Exception e) {
            e.getCause();
        }
    }

    @Test
    public void callDoubleWrapper$param1() throws Throwable {
        final TracingEventListener listener1 = new TracingEventListener();
        final TracingEventListener listener2 = new TracingEventListener();
        final Class<?> calculatorClass = JvmHelper
            .createJvm()
            .defineClass(
                NativeClass.class,
                new Transformer(NATIVECLASS_SYSTEM, listener1, BEFORE, RETURN, THROWS),
                new Transformer(NATIVECLASS_SYSTEM, listener2, BEFORE, RETURN, THROWS)
            )
            .loadClass(GaStringUtils.getJavaClassName(NativeClass.class));



        Object nativeClass = calculatorClass.newInstance();
        try {
            MethodUtils.invokeMethod(nativeClass,"currentTimeMillis");
        } catch (InvocationTargetException e) {
            assertEquals(e.getTargetException().getClass(), UnsatisfiedLinkError.class);
        } catch (Exception e) {
            e.getCause();
        }
        listener1.assertEventTracing(
            BEFORE,
            THROWS
        );
        listener2.assertEventTracing(
            BEFORE,
            THROWS
        );
    }
}
