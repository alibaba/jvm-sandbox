package com.alibaba.jvm.sandbox.qatest.core.issues;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.InvokeEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.util.JvmHelper;
import org.junit.Test;

import java.util.Stack;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.qatest.core.util.CalculatorHelper.*;
import static org.junit.Assert.assertEquals;

public class TestIssues253 {

    @Test
    public void cal$sum_add$with_AdviceListener$add_throwsImmediately_sum_catchException() throws Throwable {
        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        new JvmHelper.Transformer(
                                CALCULATOR_SUM_and_ADD_FILTER,
                                new AdviceListener() {

                                    @Override
                                    protected void afterReturning(Advice advice) throws Throwable {
                                        if (advice.getBehavior().getName().equals("add")) {
                                            ProcessController.throwsImmediately(new RuntimeException("test"));
                                        }
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        if (advice.getBehavior().getName().equals("sum")) {
                                            ProcessController.returnImmediately(100);
                                        }
                                    }
                                }
                        )
                )
                .loadClass(CALCULATOR_CLASS_NAME);

        final Object objectOfCal = newInstance(calculatorClass);
        assertEquals(100, sum(objectOfCal, 10, 20));
    }


    @Test
    public void cal$sum_add$with_EventListener$add_throwsImmediately_sum_catchException() throws Throwable {

        final Class<?> calculatorClass = JvmHelper
                .createJvm()
                .defineClass(
                        Calculator.class,
                        CALCULATOR_SUM_and_ADD_FILTER,
                        new EventListener() {

                            private final Stack<String> stack = new Stack<String>();
                            int count = 0;

                            @Override
                            public void onEvent(Event event) throws Throwable {
                                final InvokeEvent iEvent = (InvokeEvent)event;
                                switch (event.type) {
                                    case BEFORE: {
                                        stack.push(((BeforeEvent) event).javaMethodName);
                                        break;
                                    }
                                    case RETURN: {
                                        if (stack.pop().equals("add")) {
                                            ProcessController.throwsImmediately(new RuntimeException("test:"+(++count)));
                                        }
                                        break;
                                    }
                                    case THROWS: {
                                        if (stack.pop().equals("sum")) {
                                            ProcessController.returnImmediately(100);
                                        }
                                        break;
                                    }
                                }

                            }
                        },
                        BEFORE, RETURN, THROWS
                )
                .loadClass(CALCULATOR_CLASS_NAME);

        final Object objectOfCal = newInstance(calculatorClass);
        assertEquals(100, sum(objectOfCal, 10, 20));
    }

}
