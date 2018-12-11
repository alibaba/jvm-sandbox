package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.InvokeEvent;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.InterruptedEventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Test;

import java.util.Stack;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventStackTestCase extends CalculatorTestCase {

    @Test
    public void test$$stack$$around() throws Throwable {
        final Stack<Integer> processStack = new Stack<Integer>();
        final Stack<Integer> invokeStack = new Stack<Integer>();

        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_and_ADD_FILTER,
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        switch (event.type) {
                            case BEFORE:
                                final BeforeEvent beforeEvent = (BeforeEvent) event;
                                processStack.push(beforeEvent.processId);
                                invokeStack.push(beforeEvent.invokeId);
                                break;
                            default:
                                InvokeEvent invokeEvent = (InvokeEvent) event;
                                assertEquals(processStack.pop().intValue(), invokeEvent.processId);
                                assertEquals(invokeStack.pop().intValue(), invokeEvent.invokeId);
                        }

                    }

                },
                BEFORE, RETURN, THROWS
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertTrue(processStack.isEmpty());
        assertTrue(invokeStack.isEmpty());
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertTrue(processStack.isEmpty());
        assertTrue(invokeStack.isEmpty());
    }

    @Test
    public void test$$stack$$returnImmediatelyOnBefore() throws Throwable {
        final Stack<Integer> processStack = new Stack<Integer>();
        final Stack<Integer> invokeStack = new Stack<Integer>();

        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_and_ADD_FILTER,
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        switch (event.type) {
                            case BEFORE:
                                final BeforeEvent beforeEvent = (BeforeEvent) event;
                                processStack.push(beforeEvent.processId);
                                invokeStack.push(beforeEvent.invokeId);
                                if (beforeEvent.javaMethodName.equals("add")) {
                                    ProcessController.returnImmediately(30);
                                }
                                break;
                            default:
                                InvokeEvent invokeEvent = (InvokeEvent) event;
                                assertEquals(processStack.pop().intValue(), invokeEvent.processId);
                                assertEquals(invokeStack.pop().intValue(), invokeEvent.invokeId);
                        }

                    }

                },
                BEFORE, RETURN, THROWS, IMMEDIATELY_THROWS, IMMEDIATELY_RETURN
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertTrue(processStack.isEmpty());
        assertTrue(invokeStack.isEmpty());
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        assertTrue(processStack.isEmpty());
        assertTrue(invokeStack.isEmpty());
    }

    @Test
    public void test$$stack$$returnImmediatelyOnReturn() throws Throwable {
        final Stack<Integer> processStack = new Stack<Integer>();
        final Stack<Integer> invokeStack = new Stack<Integer>();

        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_and_ADD_FILTER,
                new InterruptedEventListener() {
                    boolean isIReturn = false;

                    @Override
                    public void onEvent(Event event) throws Throwable {
                        switch (event.type) {
                            case BEFORE:
                                final BeforeEvent beforeEvent = (BeforeEvent) event;
                                processStack.push(beforeEvent.processId);
                                invokeStack.push(beforeEvent.invokeId);
                                if (beforeEvent.javaMethodName.equals("add")) {
                                    isIReturn = true;
                                }
                                break;
                            default:
                                InvokeEvent invokeEvent = (InvokeEvent) event;
                                assertEquals(processStack.pop().intValue(), invokeEvent.processId);
                                assertEquals(invokeStack.pop().intValue(), invokeEvent.invokeId);
                                if (isIReturn) {
                                    ProcessController.returnImmediately(100);
                                }
                        }

                    }

                },
                BEFORE, RETURN, THROWS
        );
        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
        assertTrue(processStack.isEmpty());
        assertTrue(invokeStack.isEmpty());
//        assertEquals(100, calculatorSum(computerClass.newInstance(), 10, 20));
//        assertTrue(processStack.isEmpty());
//        assertTrue(invokeStack.isEmpty());
    }

}
