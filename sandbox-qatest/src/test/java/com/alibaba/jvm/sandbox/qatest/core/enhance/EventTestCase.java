package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.api.filter.NameRegexFilter;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

/**
 * 测试事件内容是否正确
 */
public class EventTestCase extends CalculatorTestCase {

    @Test
    public void test$$event$$before() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(BEFORE, event.type);
                        final BeforeEvent beforeEvent = (BeforeEvent) event;
                        assertEquals(1, beforeEvent.argumentArray.length);
                        assertEquals(TestClassLoader.class.getName(), beforeEvent.javaClassLoader.getClass().getName());
                        assertEquals(Calculator.class.getName(), beforeEvent.javaClassName);
                        assertEquals("sum", beforeEvent.javaMethodName);
                        assertNull(beforeEvent.target);
                        assertEquals(int[].class, beforeEvent.argumentArray[0].getClass());
                        assertEquals(10, ((int[]) beforeEvent.argumentArray[0])[0]);
                        assertEquals(20, ((int[]) beforeEvent.argumentArray[0])[1]);
                    }
                },
                BEFORE
        );
        Assert.assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
    }

    @Test
    public void test$$event$$return() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(RETURN, event.type);
                        final ReturnEvent returnEvent = (ReturnEvent) event;
                        assertEquals(30, returnEvent.object);
                    }
                },
                RETURN
        );
        Assert.assertEquals(30, calculatorSum(computerClass.newInstance(), 10, 20));
    }

    @Test(expected = RuntimeException.class)
    public void test$$event$$throws() throws Throwable {
        final Class<?> computerClass = watching(
                Calculator.class,
                new NameRegexFilter(".*", "sum"),
                new InterruptedEventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {
                        Assert.assertEquals(THROWS, event.type);
                        final ThrowsEvent throwsEvent = (ThrowsEvent) event;
                        assertEquals(RuntimeException.class, throwsEvent.throwable);
                        assertEquals("THIS IS A TEST!", throwsEvent.throwable.getMessage());
                    }
                },
                THROWS
        );
        try {
            calculatorErrorSum(computerClass.newInstance(), 10, 20);
        } catch (RuntimeException cause) {
            assertEquals("THIS IS A TEST!", cause.getMessage());
            throw cause;
        }
    }

}
