package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.filter.NameRegexFilter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils;
import com.alibaba.jvm.sandbox.core.util.UnCaughtException;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtInvokeMethod;

public class CalculatorTestCase extends CoreEnhanceBaseTestCase {

    protected int calculatorSum(final Object calculatorObject, int... numArray) {
        return unCaughtInvokeMethod(
                unCaughtGetClassDeclaredJavaMethod(calculatorObject.getClass(), "sum", int[].class),
                calculatorObject,
                numArray
        );
    }

    protected int calculatorErrorSum(final Object calculatorObject, int... numArray) throws Throwable {
        try {
            return unCaughtInvokeMethod(
                    unCaughtGetClassDeclaredJavaMethod(calculatorObject.getClass(), "errorSum", int[].class),
                    calculatorObject,
                    numArray
            );
        } catch (Throwable cause) {
            if(cause instanceof UnCaughtException
                    && (cause.getCause() instanceof InvocationTargetException)) {
                throw ((InvocationTargetException)cause.getCause()).getTargetException();
            }
            throw cause;
        }
    }

    @Test
    public void test_sum() throws IllegalAccessException, IOException, InvocationTargetException, InstantiationException {
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        final BeforeEvent beforeEvent = (BeforeEvent) event;
                        final int[] numberArray = (int[]) beforeEvent.argumentArray[0];
                        numberArray[0] = 40;
                        numberArray[1] = 60;
                    }
                },
                Event.Type.BEFORE
        );

        Assert.assertEquals(
                100,
                calculatorSum(
                        computerClass.newInstance(),
                        1, 1
                )
        );

    }

}
