package com.alibaba.jvm.sandbox.qatest.api;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.AccessFlags;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.qatest.api.mock.MockForBuilderModuleEventWatcher;
import com.alibaba.jvm.sandbox.qatest.api.mock.MockForBuilderProgress;
import com.alibaba.jvm.sandbox.qatest.api.util.ApiQaArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder.PatternType.REGEX;

public class EventWatchBuilderTestCase {


    @Test
    public void test$$EventWatchBuilder$$normal$$all() {

        final MockForBuilderModuleEventWatcher mockForBuilderModuleEventWatcher
                = new MockForBuilderModuleEventWatcher();
        final MockForBuilderProgress mockForBuilderProgress
                = new MockForBuilderProgress();
        new EventWatchBuilder(mockForBuilderModuleEventWatcher)
                .onClass(String.class)
                .onBehavior("toString")
                .onWatching()
                .withCall()
                .withLine()
                .withProgress(mockForBuilderProgress)
                .onWatch(new AdviceListener());

        Assert.assertEquals(7, mockForBuilderModuleEventWatcher.getEventTypeArray().length);
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.LINE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertEquals(1, mockForBuilderModuleEventWatcher.getEventWatchCondition().getOrFilterArray().length);
    }

    @Test
    public void test$$EventWatchBuilder$$normal$$CallOnly() {

        final MockForBuilderModuleEventWatcher mockForBuilderModuleEventWatcher
                = new MockForBuilderModuleEventWatcher();
        final MockForBuilderProgress mockForBuilderProgress
                = new MockForBuilderProgress();
        new EventWatchBuilder(mockForBuilderModuleEventWatcher)
                .onClass(String.class)
                .onBehavior("toString")
                .onWatching()
                .withCall()
                .withProgress(mockForBuilderProgress)
                .onWatch(new AdviceListener());

        Assert.assertEquals(6, mockForBuilderModuleEventWatcher.getEventTypeArray().length);
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.CALL_RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertFalse(ApiQaArrayUtils.has(Event.Type.LINE, mockForBuilderModuleEventWatcher.getEventTypeArray()));

    }

    @Test
    public void test$$EventWatchBuilder$$normal$$LineOnly() {

        final MockForBuilderModuleEventWatcher mockForBuilderModuleEventWatcher
                = new MockForBuilderModuleEventWatcher();
        final MockForBuilderProgress mockForBuilderProgress
                = new MockForBuilderProgress();
        new EventWatchBuilder(mockForBuilderModuleEventWatcher)
                .onClass(String.class)
                .onBehavior("toString")
                .onWatching()
                .withLine()
                .withProgress(mockForBuilderProgress)
                .onWatch(new AdviceListener());

        Assert.assertEquals(4, mockForBuilderModuleEventWatcher.getEventTypeArray().length);
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertFalse(ApiQaArrayUtils.has(Event.Type.CALL_BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertFalse(ApiQaArrayUtils.has(Event.Type.CALL_THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertFalse(ApiQaArrayUtils.has(Event.Type.CALL_RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.LINE, mockForBuilderModuleEventWatcher.getEventTypeArray()));

    }

    @Test
    public void test$$EventWatchBuilder$$regex() {

        final MockForBuilderModuleEventWatcher mockForBuilderModuleEventWatcher
                = new MockForBuilderModuleEventWatcher();
        new EventWatchBuilder(mockForBuilderModuleEventWatcher, REGEX)
                .onClass("java\\.lang\\.String")
                .onBehavior("<init>")
                .withParameterTypes("byte\\[\\]")
                .hasAnnotationTypes(".*Override")
                .hasExceptionTypes(".*Exception")
                .onWatch(new AdviceListener());
        Assert.assertEquals(1, mockForBuilderModuleEventWatcher.getEventWatchCondition().getOrFilterArray().length);
        final Filter filter = mockForBuilderModuleEventWatcher.getEventWatchCondition().getOrFilterArray()[0];
        Assert.assertTrue(filter.doClassFilter(
                AccessFlags.ACF_PUBLIC,
                String.class.getName(),
                null,
                null,
                null
        ));

        Assert.assertFalse(filter.doClassFilter(
                AccessFlags.ACF_PUBLIC,
                Integer.class.getName(),
                null,
                null,
                null
        ));

        Assert.assertTrue(filter.doMethodFilter(
                AccessFlags.ACF_PUBLIC,
                "<init>",
                new String[]{byte[].class.getCanonicalName()},
                new String[]{RuntimeException.class.getName(), IllegalAccessException.class.getName()},
                new String[]{Override.class.getName()}
        ));

        Assert.assertFalse(filter.doMethodFilter(
                AccessFlags.ACF_PUBLIC,
                "<cinit>",
                null,
                null,
                null
        ));

        Assert.assertFalse(filter.doMethodFilter(
                AccessFlags.ACF_PUBLIC,
                "<init>",
                new String[]{short[].class.getCanonicalName()},
                null,
                null
        ));

        Assert.assertFalse(filter.doMethodFilter(
                AccessFlags.ACF_PUBLIC,
                "<init>",
                new String[]{byte[].class.getCanonicalName()},
                new String[]{Integer.class.getName()},
                null
        ));

    }

}
