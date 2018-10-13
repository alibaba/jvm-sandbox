package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.LineEvent;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.filter.NameRegexFilter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.matchers.JUnitMatchers.*;

/**
 * 测试方法调用事件流是否正确
 */
public class EventStreamTestCase extends CalculatorTestCase {

    @Test
    public void test$$event_stream$$normal() throws Throwable {
        final Queue<Event.Type> eventQueue = new LinkedList<Event.Type>(asList(BEFORE, RETURN));
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(eventQueue.poll(), event.type);
                    }
                },
                BEFORE, RETURN, THROWS
        );

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        Assert.assertTrue(eventQueue.isEmpty());
    }

    @Test
    public void test$$event_stream$$call() throws Throwable {
        final Queue<Event.Type> eventQueue = new LinkedList<Event.Type>(asList(
                BEFORE,
                CALL_BEFORE,
                CALL_RETURN,
                CALL_BEFORE,
                CALL_RETURN,
                RETURN
        ));
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(eventQueue.poll(), event.type);
                    }
                },
                BEFORE, RETURN, THROWS, CALL_BEFORE, CALL_RETURN, CALL_THROWS
        );

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        Assert.assertTrue(eventQueue.isEmpty());
    }

    @Test
    public void test$$event_stream$$line() throws Throwable {
        final Queue<Event.Type> eventQueue = new LinkedList<Event.Type>(asList(
                BEFORE,
                LINE, // int sum = 0;
                LINE, // for (int num : numArray) # index=0
                LINE, //     sum = add(sum, num);
                LINE, // for (int num : numArray) # index=1
                LINE, //     sum = add(sum, num);
                LINE, // for (int num : numArray) # index=2
                LINE, // return sum;
                RETURN
        ));
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(eventQueue.poll(), event.type);
                    }
                },
                BEFORE, RETURN, THROWS, LINE
        );

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        Assert.assertTrue(eventQueue.isEmpty());
    }

    @Test
    public void test$$event_stream$$all() throws Throwable {
        final Queue<Event.Type> eventQueue = new LinkedList<Event.Type>(asList(
                BEFORE,
                LINE, // int sum = 0;
                LINE, // for (int num : numArray) # index=0
                LINE, //     sum = add(sum, num);
                CALL_BEFORE,
                CALL_RETURN,
                LINE, // for (int num : numArray) # index=1
                LINE, //     sum = add(sum, num);
                CALL_BEFORE,
                CALL_RETURN,
                LINE, // for (int num : numArray) # index=2
                LINE, // return sum;
                RETURN
        ));
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    int index = 0;

                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertThat("@queue#index=" + index++, eventQueue.poll(), equalTo(event.type));
                    }
                },
                Event.Type.values()
        );

        Assert.assertEquals(2, calculatorSum(computerClass.newInstance(), 1, 1));
        Assert.assertTrue(eventQueue.isEmpty());
    }

}
