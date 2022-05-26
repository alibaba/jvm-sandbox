package com.alibaba.jvm.sandbox.qatest.core.issues;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.MyCalculator;
import com.alibaba.jvm.sandbox.qatest.core.mock.EmptyModuleEventWatcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder.PatternType.REGEX;
import static com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureFactory.createClassStructure;
import static com.alibaba.jvm.sandbox.qatest.core.util.QaClassUtils.toByteArray;

public class TestIssues217 {

    class GetMatcherModuleEventWatcher extends EmptyModuleEventWatcher {

        private EventWatchCondition condition;

        @Override
        public int watch(EventWatchCondition condition, EventListener listener, Progress progress, Event.Type... eventType) {
            this.condition = condition;
            return super.watch(condition, listener, progress, eventType);
        }

        public EventWatchCondition getCondition() {
            return condition;
        }

        public Matcher getMatcher() {
            return ExtFilterMatcher.toOrGroupMatcher(condition.getOrFilterArray());
        }

    }

    @Test
    public void matchingComputerAnnotation() throws IOException {


        final GetMatcherModuleEventWatcher watcher = new GetMatcherModuleEventWatcher();

        new EventWatchBuilder(watcher)
                .onClass("*")
                .hasAnnotationTypes("com.alibaba.jvm.sandbox.qatest.core.enhance.target.Computer")
                .onAnyBehavior()
                .onWatch(new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {

                    }
                });

        final Matcher matcher = watcher.getMatcher();
        Assert.assertTrue(matcher.matching(createClassStructure(Calculator.class)).isMatched());
        Assert.assertTrue(matcher.matching(createClassStructure(toByteArray(Calculator.class), getClass().getClassLoader())).isMatched());

        Assert.assertFalse(matcher.matching(createClassStructure(MyCalculator.class)).isMatched());
        Assert.assertFalse(matcher.matching(createClassStructure(toByteArray(MyCalculator.class), getClass().getClassLoader())).isMatched());

        Assert.assertFalse(matcher.matching(createClassStructure(TestIssues217.class)).isMatched());
        Assert.assertFalse(matcher.matching(createClassStructure(toByteArray(TestIssues217.class), getClass().getClassLoader())).isMatched());

    }

    @Test
    public void matchingInheritedComputerAnnotation() throws IOException {


        final GetMatcherModuleEventWatcher watcher = new GetMatcherModuleEventWatcher();

        new EventWatchBuilder(watcher)
                .onClass("*")
                .hasAnnotationTypes("com.alibaba.jvm.sandbox.qatest.core.enhance.target.InheritedComputer")
                .onAnyBehavior()
                .onWatch(new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {

                    }
                });

        final Matcher matcher = watcher.getMatcher();
        Assert.assertTrue(matcher.matching(createClassStructure(Calculator.class)).isMatched());
        Assert.assertTrue(matcher.matching(createClassStructure(toByteArray(Calculator.class), getClass().getClassLoader())).isMatched());

        Assert.assertTrue(matcher.matching(createClassStructure(MyCalculator.class)).isMatched());
        Assert.assertTrue(matcher.matching(createClassStructure(toByteArray(MyCalculator.class), getClass().getClassLoader())).isMatched());

        Assert.assertFalse(matcher.matching(createClassStructure(TestIssues217.class)).isMatched());
        Assert.assertFalse(matcher.matching(createClassStructure(toByteArray(TestIssues217.class), getClass().getClassLoader())).isMatched());

    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAAnnotation {

    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestBAnnotation {

    }

    @TestAAnnotation
    @TestBAnnotation
    class TestAB {

    }

    @TestAAnnotation
    class TestA {

    }

    @TestBAnnotation
    class TestB {

    }

    @Test
    public void matching__TestA_or_TestB__Annotation() {

        final GetMatcherModuleEventWatcher watcher = new GetMatcherModuleEventWatcher();

        new EventWatchBuilder(watcher, REGEX)
                .onAnyClass()
                .hasAnnotationTypes(".*Test(A|B)Annotation")
                .onAnyBehavior()
                .onWatch(new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Throwable {

                    }
                });

        final Matcher matcher = watcher.getMatcher();
        Assert.assertTrue(matcher.matching(createClassStructure(TestA.class)).isMatched());
        Assert.assertTrue(matcher.matching(createClassStructure(TestB.class)).isMatched());
        Assert.assertTrue(matcher.matching(createClassStructure(TestAB.class)).isMatched());

    }

}
