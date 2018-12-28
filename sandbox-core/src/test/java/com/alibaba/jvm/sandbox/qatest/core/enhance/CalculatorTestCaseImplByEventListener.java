package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.TracingEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.ProcessController.returnImmediately;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.*;
import static org.junit.Assert.assertEquals;

public class CalculatorTestCaseImplByEventListener implements ICalculatorTestCase {

    @Test
    @Override
    public void cal$sum$around() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_FILTER,
                        listener = new TracingEventListener(),
                        BEFORE, RETURN, THROWS
                )
                .loadClass(CALCULATOR_CLASS_NAME);

        assertEquals(30, sum(newInstance(calculatorClass), 10,20));
        listener.assertEventTracing(
                BEFORE,
                RETURN
        );

    }

    @Override
    public void cal$sum$line() throws Throwable {

    }

    @Override
    public void cal$sum$call() throws Throwable {

    }

    @Override
    public void cal$sum$before$changeParameters() throws Throwable {

    }

    @Override
    public void cal$sum$before$returnImmediately() throws Throwable {

    }

    @Override
    public void cal$sum$before$throwsImmediately() throws Throwable {

    }

    @Override
    public void cal$sum$return$changeParameters() throws Throwable {

    }

    @Test
    @Override
    public void cal$sum$return$returnImmediately() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_FILTER,
                        listener = new TracingEventListener(){
                            @Override
                            public void onEvent(Event event) throws Throwable {
                                super.onEvent(event);
                                returnImmediately(100);
                            }
                        },
                        RETURN
                )
                .loadClass(CALCULATOR_CLASS_NAME);

        assertEquals(100, sum(newInstance(calculatorClass), 10,20));
        listener.assertEventTracing(
                RETURN
        );
    }

    @Override
    public void cal$sum$return$throwsImmediately() throws Throwable {

    }

    @Override
    public void cal$sum$throws$changeParameters() throws Throwable {

    }

    @Override
    public void cal$sum$throws$returnImmediately() throws Throwable {

    }

    @Override
    public void cal$sum$throws$throwsImmediately() throws Throwable {

    }

    @Override
    public void cal$sum_add$around() throws Throwable {

    }

    @Override
    public void cal$sum_add$line() throws Throwable {

    }

    @Override
    public void cal$sum_add$call() throws Throwable {

    }

    @Override
    public void cal$sum_add$before$changeParameters_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$before$returnImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$before$throwsImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$return$changeParameters_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$return$returnImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$return$throwsImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$throws$changeParameters_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$throws$returnImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$sum_add$throws$throwsImmediately_at_add() throws Throwable {

    }

    @Override
    public void cal$pow$around() throws Throwable {

    }

    @Override
    public void cal$pow$line() throws Throwable {

    }

    @Override
    public void cal$pow$call() throws Throwable {

    }

    @Override
    public void cal$init_with_TestCase$around() throws Throwable {

    }

    @Override
    public void cal$init_with_TestCase$line() throws Throwable {

    }

    @Override
    public void cal$init_with_TestCase$call() throws Throwable {

    }

    @Override
    public void cal$init_with_TestCase$before$changeParameters() throws Throwable {

    }
}
