package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.ProcessController;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.RETURN;
import static com.alibaba.jvm.sandbox.api.event.Event.Type.THROWS;
import static junit.framework.Assert.assertEquals;

/**
 * 测试Namespace
 */
public class NamespaceTestCase extends CalculatorTestCase {

    @Test
    public void test$$namespace$$changeReturnThenWatching() throws Throwable {
        final EventStreamCheckerListener changeReturnListener;
        final Class<?> changeReturnCalculatorClass = watchingWithNamespace(
                "ns-change",
                Calculator.class,
                CALCULATOR_ERROR_SUM_FILTER,
                changeReturnListener = new EventStreamCheckerListener().nextEventCheck(new EventStreamCheckerListener.EventChecker<ThrowsEvent>() {
                    @Override
                    public void onCheck(ThrowsEvent event) throws Throwable {
                        ProcessController.returnImmediately(100);
                    }
                }),
                THROWS
        );

        final EventStreamCheckerListener watchingListener;
        final Class<?> watchingCalculatorClass = watchingWithNamespace(
                "ns-watching",
                changeReturnCalculatorClass,
                CALCULATOR_ERROR_SUM_FILTER,
                watchingListener = new EventStreamCheckerListener().nextEventCheck(new EventStreamCheckerListener.EventChecker<ReturnEvent>() {
                    @Override
                    public void onCheck(ReturnEvent event) throws Throwable {
                        assertEquals(event.object, 100);
                    }
                }),
                RETURN
        );

        assertEquals(100, calculatorErrorSum(watchingCalculatorClass.newInstance(), 1, 1));
        changeReturnListener.assertIsEmpty();
        watchingListener.assertIsEmpty();
    }

}
