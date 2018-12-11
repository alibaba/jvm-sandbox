package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceAdapterListener;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.InterruptedAdviceAdapterListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
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
                        assertEquals(10, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[])advice.getParameterArray()[0])[1]);
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
                        assertEquals(10, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[])advice.getParameterArray()[0])[1]);
                        advice.changeParameter(0, new int[]{40,60});
                    }

                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        assertEquals(40, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(60, ((int[])advice.getParameterArray()[0])[1]);
                        assertEquals(100, advice.getReturnObj());;
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
                        assertEquals(10, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[])advice.getParameterArray()[0])[1]);
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
                        assertEquals(10, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[])advice.getParameterArray()[0])[1]);
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
                        assertEquals(10, ((int[])advice.getParameterArray()[0])[0]);
                        assertEquals(20, ((int[])advice.getParameterArray()[0])[1]);
                        ProcessController.returnImmediately(100);
                    }

                }),
                BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS
        );
        assertEquals(100, calculatorErrorSum(computerClass.newInstance(), 10, 20));
        assertEquals(100, calculatorErrorSum(computerClass.newInstance(), 10, 20));
    }

}
