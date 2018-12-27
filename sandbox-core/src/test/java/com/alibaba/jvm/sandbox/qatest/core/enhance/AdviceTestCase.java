package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceAdapterListener;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.util.ObjectIDs;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.InterruptedAdviceAdapterListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper.Transformer;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.getJavaClassName;
import static org.junit.Assert.*;

public class AdviceTestCase extends CalculatorTestCase {

    @Test
    public void test$$advice$$aroundWithError() throws Throwable {
        final StringBuilder traceSB = new StringBuilder();
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                new AdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        traceSB.append("before;");
                        throw new RuntimeException("TEST");
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        traceSB.append("return;");
                        throw new RuntimeException("TEST");
                    }
                }),
                BEFORE, RETURN, THROWS
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertEquals("before;return;", traceSB.toString());
    }

    @Test
    public void test$$advice$$around() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                new InterruptedAdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        assertEquals(10, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[]) advice.getParameterArray()[0])[1]);
                        assertTrue(advice.isProcessTop());
                        assertFalse(advice.isReturn());
                        assertFalse(advice.isThrows());
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        assertEquals(30, advice.getReturnObj());
                        assertTrue(advice.isProcessTop());
                        assertTrue(advice.isReturn());
                        assertFalse(advice.isThrows());
                    }
                }),
                BEFORE, RETURN, THROWS
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
    }


    @Test
    public void test$$advice$$changeParameters() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                new InterruptedAdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        assertEquals(10, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[]) advice.getParameterArray()[0])[1]);
                        advice.changeParameter(0, new int[]{40, 60});
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        assertEquals(40, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(60, ((int[]) advice.getParameterArray()[0])[1]);
                        assertEquals(100, advice.getReturnObj());
                        ;
                    }
                }),
                BEFORE, RETURN, THROWS
        );
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
    }

    @Test
    public void test$$advice$$changeReturnOnBefore() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                new InterruptedAdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        assertTrue(advice.isProcessTop());
                        assertEquals(10, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[]) advice.getParameterArray()[0])[1]);
                        ProcessController.returnImmediately(100);
                    }

                }),
                BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
        );
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
    }

    @Test
    public void test$$advice$$changeReturnOnReturn() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                new InterruptedAdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        assertTrue(advice.isProcessTop());
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        assertTrue(advice.isProcessTop());
                        assertEquals(10, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[]) advice.getParameterArray()[0])[1]);
                        ProcessController.returnImmediately(100);
                    }

                }),
                BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
        );
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
    }

    @Test
    public void test$$advice$$changeReturnOnThrows() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_ERROR_SUM_FILTER,
                new InterruptedAdviceAdapterListener(new AdviceListener() {

                    @Override
                    protected void before(Advice advice) throws Throwable {
                        assertTrue(advice.isProcessTop());
                    }

                    @Override
                    protected void afterThrowing(Advice advice) throws Throwable {
                        assertTrue(advice.isProcessTop());
                        assertEquals(10, ((int[]) advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[]) advice.getParameterArray()[0])[1]);
                        ProcessController.returnImmediately(100);
                    }

                }),
                BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
        );
        assertEquals(100, calculatorErrorSum(computerClass.newInstance(), 10, 20));
        assertEquals(100, calculatorErrorSum(computerClass.newInstance(), 10, 20));
    }

    @Test
    public void test$$advice$$sum_add$$changeThrowsOnReturn() throws Throwable {
        final AtomicBoolean isThrowingAtSum = new AtomicBoolean(false);
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        new InterruptedAdviceAdapterListener(new AdviceListener() {

                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (advice.getBehavior().getName().equals("sum")) {
                                    assertTrue(advice.isProcessTop());
                                } else {
                                    assertFalse(advice.isProcessTop());
                                    ProcessController.throwsImmediately(new IllegalStateException("TEST"));
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                isThrowingAtSum.set(true);
                                assertEquals("sum", advice.getBehavior().getName());
                                ProcessController.returnImmediately(100);
                            }
                        }),
                        BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
                )
                .loadClass(getJavaClassName(Calculator.class));

        assertEquals(100, calculatorSum(calculatorClass.newInstance(), 10, 20));

    }

    @Test
    public void test$$immediately$$throw_return_return() throws Throwable {

        final EventListener listener1;
        final EventListener listener2;
        final EventListener listener3;
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        new Transformer(
                                CALCULATOR_SUM_FILTER,
                                listener1 = new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Throwable {
                                        ProcessController.throwsImmediately(new RuntimeException());
                                    }
                                },
                                BEFORE
                        ),
                        new Transformer(
                                CALCULATOR_SUM_FILTER,
                                listener2 = new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Throwable {
                                        ProcessController.returnImmediately(100);
                                    }
                                },
                                THROWS
                        ),
                        new Transformer(
                                CALCULATOR_SUM_FILTER,
                                listener3 = new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Throwable {
                                        final ReturnEvent returnEvent = (ReturnEvent) event;
                                        ProcessController.returnImmediately(10 + (Integer) returnEvent.object);
                                    }
                                },
                                RETURN
                        )
                )
                .loadClass(getJavaClassName(Calculator.class));

        assertEquals(110, calculatorSum(calculatorClass.newInstance(), 10, 20));
        EventListenerHandlers.getSingleton().checkEventProcessor(
                ObjectIDs.instance.identity(listener1),
                ObjectIDs.instance.identity(listener2),
                ObjectIDs.instance.identity(listener3)
        );
    }

}
