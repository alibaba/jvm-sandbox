package com.alibaba.jvm.sandbox.module.junit.tests;

import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.probe.DummyModule;
import com.alibaba.jvm.sandbox.module.junit.rule.OutputCapture;
import com.alibaba.jvm.sandbox.module.junit.rule.SandboxRule;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 3:29 下午
 */
public class RuleCase3 {
    @Rule
    public SandboxRule sandboxRule = new SandboxRule();

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Test
    @PrepareForTest(testModule = DummyModule.class, spyClasses = String.class)
    public void testcase1() {

        String test = new String("password");

        System.out.println(test);

        String result = outputCapture.toString();

        assertThat(result, Matchers.not(containsString("has password keyword")));
    }

    @Test
    @PrepareForTest(testModule = DummyModule.class, spyClasses = String.class, isEnableUnsafe = true)
    public void testcase2() {

        String test = new String("password");

        System.out.println(test);

        String result = outputCapture.toString();

        assertThat(result, containsString("has password keyword"));
    }

    @Test
    @PrepareForTest(testModule = DummyModule.class, fastMode = false)
    public void testcase3() {

        String test = new String("password");

        System.out.println(test);

        String result = outputCapture.toString();

        assertThat(result, containsString("has password keyword"));
    }

}
