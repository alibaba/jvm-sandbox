package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.util.GaStringUtils;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.TracingEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.NativeClass;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper.Transformer;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.*;

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

    @Test(expected = UnsatisfiedLinkError.class)
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
            throw e.getTargetException();
            // assertEquals(e.getTargetException().getClass(), UnsatisfiedLinkError.class);
        }
        Assert.fail("not arrive");
    }

    @Test(expected = UnsatisfiedLinkError.class)
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
            throw e.getTargetException();
            // assertEquals(e.getTargetException().getClass(), UnsatisfiedLinkError.class);
        }
        Assert.fail("not arrive");
    }
}
