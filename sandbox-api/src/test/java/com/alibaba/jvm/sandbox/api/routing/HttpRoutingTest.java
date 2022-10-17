package com.alibaba.jvm.sandbox.api.routing;

import com.alibaba.jvm.sandbox.api.routing.RoutingInfo.Type;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link HttpRoutingTest}
 * <p>
 *
 * @author zhaoyb1990
 */
public class HttpRoutingTest {

    @Test
    public void testUsingSecondary() {
        HttpRouting httpRouting = new HttpRouting();
        assertNull(httpRouting.appRouting());
        RoutingInfo specialRouting = httpRouting.getSpecialRouting();
        assertNotNull(specialRouting);
        assertEquals(specialRouting.getType(), Type.TARGET_CLASS);
        assertEquals(specialRouting.getTargetClass(), "javax.servlet.http.HttpServlet");
    }

    @Test
    public void testUsingApp() {
        System.setProperty("sun.java.command", "org.springframework.boot.loader.JarLauncher start");
        HttpRouting httpRouting = new HttpRouting();
        assertNotNull(httpRouting.appRouting());
        RoutingInfo specialRouting = httpRouting.getSpecialRouting();
        assertNotNull(specialRouting);
        assertEquals(specialRouting.getType(), Type.TARGET_CLASS_LOADER_NAME);
        assertEquals(specialRouting.getTargetClassLoaderName(), "org.springframework.boot.loader.LaunchedURLClassLoader");
    }
}