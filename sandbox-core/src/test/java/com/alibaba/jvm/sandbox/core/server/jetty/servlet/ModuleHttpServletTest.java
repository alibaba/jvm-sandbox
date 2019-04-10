package com.alibaba.jvm.sandbox.core.server.jetty.servlet;

import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import org.junit.Before;
import org.junit.Test;

public class ModuleHttpServletTest {
    private ModuleHttpServlet moduleHttpServlet;

    @Before
    public void setUp() throws Exception {
        CoreModuleManager coreModuleManager = null;
        moduleHttpServlet = new ModuleHttpServlet(coreModuleManager);
    }

    @Test
    public void doGet() {
    }

    @Test
    public void doPost() {
    }
}