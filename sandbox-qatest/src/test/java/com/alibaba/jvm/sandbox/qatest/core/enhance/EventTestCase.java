package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.*;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener.EventChecker;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static com.alibaba.jvm.sandbox.qatest.core.enhance.listener.EventStreamCheckerListener.EventTypeChecker.CALL_RETURN_CHECKER;
import static org.junit.Assert.*;

/**
 * 测试事件内容是否正确
 */
public class EventTestCase extends CalculatorTestCase {

    @Test
    public void test$$event$$before() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<BeforeEvent>() {
                    @Override
                    public void onCheck(BeforeEvent event) {
                        assertEquals(BEFORE, event.type);
                        assertEquals(1, event.argumentArray.length);
                        assertNotNull(event.javaClassLoader);
                        assertEquals(Calculator.class.getName(), event.javaClassName);
                        assertEquals("sum", event.javaMethodName);
                        assertNull(event.target);
                        assertEquals(int[].class, event.argumentArray[0].getClass());
                        assertEquals(10, ((int[]) event.argumentArray[0])[0]);
                        assertEquals(20, ((int[]) event.argumentArray[0])[1]);
                    }
                }),
                BEFORE
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        listener.assertIsEmpty();
    }

    @Test
    public void test$$event$$return() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<ReturnEvent>() {
                    @Override
                    public void onCheck(ReturnEvent event) {
                        assertEquals(RETURN, event.type);
                        assertEquals(30, event.object);
                    }
                }),
                RETURN
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        listener.assertIsEmpty();
    }


    @Test(expected = RuntimeException.class)
    public void test$$event$$throws() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_ERROR_SUM_FILTER,
                listener = new EventStreamCheckerListener().nextEventCheck(new EventChecker<ThrowsEvent>() {
                    @Override
                    public void onCheck(ThrowsEvent event) {
                        assertEquals(THROWS, event.type);
                        assertEquals(RuntimeException.class, event.throwable.getClass());
                        assertEquals("THIS IS A TEST!", event.throwable.getMessage());
                    }
                }),
                THROWS
        );
        assertCalculatorErrorSum(listener, computerClass);
    }

    private void assertCalculatorErrorSum(EventStreamCheckerListener listener, Class<?> computerClass) throws Throwable {
        try {
            calculatorErrorSum(computerClass.newInstance(), 10, 20);
            assertFalse("must throw exception", true);
        } catch (RuntimeException cause) {
            assertEquals("THIS IS A TEST!", cause.getMessage());
            listener.assertIsEmpty();
            throw cause;
        }
    }

    @Test
    public void test$$event$$call_before() throws Throwable {

        final EventChecker<CallBeforeEvent> callBeforeEventEventChecker = new EventChecker<CallBeforeEvent>() {
            @Override
            public void onCheck(CallBeforeEvent event) {
                assertEquals(CALL_BEFORE, event.type);
                assertEquals(Calculator.class.getName(), event.owner);
                assertEquals("add", event.name);
                assertEquals("(II)I", event.desc);
                assertEquals(43, event.lineNumber);
            }
        };

        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(callBeforeEventEventChecker)
                        .nextEventCheck(callBeforeEventEventChecker),
                CALL_BEFORE
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        listener.assertIsEmpty();
    }

    @Test
    public void test$$event$$call_return() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(CALL_RETURN_CHECKER)
                        .nextEventCheck(CALL_RETURN_CHECKER),
                CALL_RETURN
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        listener.assertIsEmpty();
    }

    @Test(expected = RuntimeException.class)
    public void test$$event$$call_throws() throws Throwable {
        final EventChecker<CallThrowsEvent> callThrowsEventEventChecker = new EventChecker<CallThrowsEvent>() {
            @Override
            public void onCheck(CallThrowsEvent event) {
                assertEquals(CALL_THROWS, event.type);
                assertEquals(RuntimeException.class.getName(), event.throwException);
            }
        };

        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_ERROR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(callThrowsEventEventChecker),
                CALL_THROWS
        );
        assertCalculatorErrorSum(listener, computerClass);
    }


    private class LineEventChecker implements EventChecker<LineEvent> {

        private final int lineNumber;

        private LineEventChecker(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public void onCheck(LineEvent event) {
            assertEquals(LINE, event.type);
            assertEquals(lineNumber, event.lineNumber);
        }
    }

    @Test
    public void test$$event$$line() throws Throwable {
        EventStreamCheckerListener listener;
        final Class<?> computerClass = watching(
                Calculator.class,
                CALCULATOR_SUM_FILTER,
                listener = new EventStreamCheckerListener()
                        .nextEventCheck(new LineEventChecker(41))
                        .nextEventCheck(new LineEventChecker(42))
                        .nextEventCheck(new LineEventChecker(43))
                        .nextEventCheck(new LineEventChecker(42))
                        .nextEventCheck(new LineEventChecker(43))
                        .nextEventCheck(new LineEventChecker(42))
                        .nextEventCheck(new LineEventChecker(45))
                ,
                LINE
        );
        assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
        listener.assertIsEmpty();
    }

}
