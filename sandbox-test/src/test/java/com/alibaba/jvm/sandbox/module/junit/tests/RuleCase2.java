package com.alibaba.jvm.sandbox.module.junit.tests;

import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.probe.DummyModule;
import com.alibaba.jvm.sandbox.module.junit.rule.OutputCapture;
import com.alibaba.jvm.sandbox.module.junit.rule.SandboxRule;
import com.alibaba.jvm.sandbox.module.junit.runner.SandboxRunner;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import servlet.cases.DummyServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 2:52 下午
 */
public class RuleCase2 {

    private HttpServletRequest request;

    private HttpServletResponse response;

    @Rule
    public SandboxRule sandboxRule = new SandboxRule();

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Before
    public void before() {

        // mock http servlet
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(request.getRemoteAddr()).thenReturn("10.10.10.10");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("http://mytest.com");
        when(request.getHeader("User-Agent")).thenReturn("User-Agent");
        when(request.getParameterMap()).thenReturn(Maps.<String, String[]>newHashMap());

    }

    @Test
    @PrepareForTest(testModule = DummyModule.class, spyClasses = DummyServlet.class)
    public void testcaseWithFastMode() throws ServletException, IOException {

        when(request.getHeader("username")).thenReturn("admin");

        HttpServlet servlet = new DummyServlet();

        servlet.service(request, response);

        String result = outputCapture.toString();

        assertThat(result, containsString("Admin login"));
    }

}
