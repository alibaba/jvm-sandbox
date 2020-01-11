package com.alibaba.jvm.sandbox.qatest.api.issues;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.qatest.api.mock.MockForBuilderModuleEventWatcher;
import com.alibaba.jvm.sandbox.qatest.api.util.ApiQaArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.api.event.Event.Type.CALL_BEFORE;

public class TestIssues256 {

    @Test
    public void test$$onBehavior$$onWatch$without_special_EventType() {

        final MockForBuilderModuleEventWatcher mockForBuilderModuleEventWatcher
                = new MockForBuilderModuleEventWatcher();

        new EventWatchBuilder(mockForBuilderModuleEventWatcher)
                .onClass(String.class)
                .onBehavior("toString")
                .onWatch(new AdviceListener(), CALL_BEFORE);

        Assert.assertEquals(5, mockForBuilderModuleEventWatcher.getEventTypeArray().length);
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.BEFORE, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.IMMEDIATELY_RETURN, mockForBuilderModuleEventWatcher.getEventTypeArray()));
        Assert.assertTrue(ApiQaArrayUtils.has(Event.Type.IMMEDIATELY_THROWS, mockForBuilderModuleEventWatcher.getEventTypeArray()));

    }

}
