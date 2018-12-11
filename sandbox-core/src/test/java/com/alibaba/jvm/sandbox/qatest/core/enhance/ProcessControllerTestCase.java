package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener.EventChecker;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static junit.framework.Assert.assertEquals;

/**
 * 事件流程控制测试用例
 */
public class ProcessControllerTestCase extends CalculatorTestCase {

    @Test
    public void test$$process_controller$$changeParameters() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<BeforeEvent>() {
                    @Override
                    public void onCheck(BeforeEvent event) {
                        event.changeParameter(0, new int[]{40, 60});
                    }
                }),
                BEFORE
        );

        assertEquals(100, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();

    }

    @Test
    public void test$$process_controller$$changeReturnOnBefore() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<BeforeEvent>() {
                    @Override
                    public void onCheck(BeforeEvent event) throws Throwable {
                        ProcessController.returnImmediately(100);
                    }
                }),
                BEFORE
        );

        assertEquals(100, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }


    @Test
    public void test$$process_controller$$changeReturnOnReturn() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<ReturnEvent>() {
                    @Override
                    public void onCheck(ReturnEvent event) throws Throwable {
                        assertEquals(event.object, 2);
                        ProcessController.returnImmediately(100);
                    }
                }),
                RETURN
        );

        assertEquals(100, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

    @Test
    public void test$$process_controller$$changeReturnOnThrows() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_ERROR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<ThrowsEvent>() {
                    @Override
                    public void onCheck(ThrowsEvent event) throws Throwable {
                        ProcessController.returnImmediately(100);
                    }
                }),
                THROWS
        );

        assertEquals(100, calculatorErrorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

}
