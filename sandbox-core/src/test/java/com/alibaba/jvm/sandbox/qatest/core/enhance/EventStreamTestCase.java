package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener.EventTypeChecker.*;

/**
 * 测试方法调用事件流是否正确
 */
public class EventStreamTestCase extends CalculatorTestCase {

    @Test
    public void test$$event_stream$$normal() throws Throwable {
        final EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(BEFORE_CHECKER)
                        .nextEventCheck(RETURN_CHECKER),
                BEFORE, RETURN, THROWS
        );

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
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

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
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

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
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

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        listener.assertIsEmpty();
    }

}
