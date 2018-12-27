package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.TracingEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.ProcessController.throwsImmediately;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.getJavaClassName;
import static com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener.EventTypeChecker.*;
import static org.junit.Assert.assertEquals;

/**
 * 测试方法调用事件流是否正确
 */
public class EventStreamTestCase extends CalculatorTestCase {


    @Test
    public void sum$return() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_FILTER,
                        listener = new TracingEventListener(),
                        BEFORE, RETURN, THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        assertEquals(20, calculatorSum(calculatorClass.newInstance(), 10, 10));
        listener.assertEventTracing(
                BEFORE,
                RETURN
        );

    }

    @Test
    public void sum_add$return() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        listener = new TracingEventListener(),
                        BEFORE, RETURN, THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        assertEquals(20, calculatorSum(calculatorClass.newInstance(), 10, 10));
        listener.assertEventTracing(
                BEFORE,
                BEFORE,
                RETURN,
                BEFORE,
                RETURN,
                RETURN
        );

    }

    @Test(expected = RuntimeException.class)
    public void sum_add$throwsImmediately_at_add_before() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        listener = new TracingEventListener() {
                            @Override
                            public void onEvent(Event event) throws Throwable {
                                super.onEvent(event);
                                if (event.type == BEFORE) {
                                    final BeforeEvent beforeEvent = (BeforeEvent) event;
                                    if (beforeEvent.javaMethodName.equals("add")) {
                                        throwsImmediately(new RuntimeException("TEST"));
                                    }
                                }
                            }
                        },
                        BEFORE, RETURN, THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        try {
            calculatorSum(calculatorClass.newInstance(), 10, 10);
        } finally {
            listener.assertEventTracing(
                    BEFORE,
                    BEFORE,
                    THROWS
            );
        }
    }

    @Test(expected = RuntimeException.class)
    public void sum_add$throwsImmediately_at_add_before$with_immediately() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        listener = new TracingEventListener() {
                            @Override
                            public void onEvent(Event event) throws Throwable {
                                super.onEvent(event);
                                if (event.type == BEFORE) {
                                    final BeforeEvent beforeEvent = (BeforeEvent) event;
                                    if (beforeEvent.javaMethodName.equals("add")) {
                                        throwsImmediately(new RuntimeException("TEST"));
                                    }
                                }
                            }
                        },
                        BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        try {
            calculatorSum(calculatorClass.newInstance(), 10, 10);
        } finally {
            listener.assertEventTracing(
                    BEFORE,
                    BEFORE,
                    IMMEDIATELY_THROWS,
                    THROWS
            );
        }
    }


    @Test(expected = RuntimeException.class)
    public void sum_add$throwsImmediately_at_add_return() throws Throwable {
        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        listener = new TracingEventListener() {

                            int firstAddInvokeId = -1;

                            @Override
                            public void onEvent(Event event) throws Throwable {
                                super.onEvent(event);
                                switch (event.type) {
                                    case BEFORE: {
                                        final BeforeEvent beforeEvent = (BeforeEvent) event;
                                        if (beforeEvent.javaMethodName.equals("add")) {
                                            firstAddInvokeId = beforeEvent.invokeId;
                                        }
                                        break;
                                    }
                                    case RETURN: {
                                        final ReturnEvent returnEvent = (ReturnEvent) event;
                                        if (returnEvent.invokeId == firstAddInvokeId) {
                                            throwsImmediately(new RuntimeException("TEST"));
                                        }
                                        break;
                                    }
                                }
                            }
                        },
                        BEFORE, RETURN, THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        try {
            calculatorSum(calculatorClass.newInstance(), 10, 10);
        } finally {
            listener.assertEventTracing(
                    BEFORE,
                    BEFORE,
                    RETURN,
                    THROWS
            );
        }
    }

    @Test(expected = RuntimeException.class)
    public void sum_add$throwsImmediately_at_add_return$with_immediately() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        listener = new TracingEventListener() {

                            int firstAddInvokeId = -1;

                            @Override
                            public void onEvent(Event event) throws Throwable {
                                super.onEvent(event);
                                switch (event.type) {
                                    case BEFORE: {
                                        final BeforeEvent beforeEvent = (BeforeEvent) event;
                                        if (beforeEvent.javaMethodName.equals("add")) {
                                            firstAddInvokeId = beforeEvent.invokeId;
                                        }
                                        break;
                                    }
                                    case RETURN: {
                                        final ReturnEvent returnEvent = (ReturnEvent) event;
                                        if (returnEvent.invokeId == firstAddInvokeId) {
                                            throwsImmediately(new RuntimeException("TEST"));
                                        }
                                        break;
                                    }
                                }
                            }

                        },
                        BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        try {
            calculatorSum(calculatorClass.newInstance(), 10, 10);
        } finally {
            listener.assertEventTracing(
                    BEFORE,
                    BEFORE,
                    RETURN,
                    IMMEDIATELY_THROWS,
                    THROWS
            );
        }
    }


    @Test(expected = Exception.class)
    public void sum$throws() throws Throwable {

        final TracingEventListener listener;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_ERROR_SUM_FILTER,
                        listener = new TracingEventListener(),
                        BEFORE, RETURN, THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        try {
            calculatorErrorSum(calculatorClass.newInstance(), 10, 10);
        } finally {
            listener.assertEventTracing(
                    BEFORE,
                    THROWS
            );
        }

    }


    @Test
    public void test$$event_stream$$call() throws Throwable {
        final EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(BEFORE_CHECKER)
                        .nextEventCheck(CALL_BEFORE_CHECKER)
                        .nextEventCheck(CALL_RETURN_CHECKER)
                        .nextEventCheck(CALL_BEFORE_CHECKER)
                        .nextEventCheck(CALL_RETURN_CHECKER)
                        .nextEventCheck(RETURN_CHECKER),
                BEFORE, RETURN, THROWS, CALL_BEFORE, CALL_RETURN, CALL_THROWS
        );

        assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

    @Test
    public void test$$event_stream$$line() throws Throwable {
        final EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(BEFORE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(RETURN_CHECKER),
                BEFORE, RETURN, THROWS, LINE
        );

        assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

    @Test
    public void test$$event_stream$$all() throws Throwable {
        final EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(BEFORE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(CALL_BEFORE_CHECKER)
                        .nextEventCheck(CALL_RETURN_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(CALL_BEFORE_CHECKER)
                        .nextEventCheck(CALL_RETURN_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(LINE_CHECKER)
                        .nextEventCheck(RETURN_CHECKER),
                Event.Type.values()
        );

        assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

}
