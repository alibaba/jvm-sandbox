package com.alibaba.jvm.sandbox.api.routing;

import com.alibaba.jvm.sandbox.api.routing.RoutingInfo.Type;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link LaunchedRoutingTest}
 * <p>
 *
 * @author zhaoyb
 */
public class LaunchedRoutingTest {

    @Test
    public void testNoLaunchedRoutingMatch() {
        System.setProperty("sun.java.command", "");
        RoutingInfo routingInfo = LaunchedRouting.useLaunchingRouting("^com.alibaba.jvm.sandbox.*");
        assertNull(routingInfo);
    }

    @Test
    public void testMatchingTomcat() {
        System.setProperty("sun.java.command", "org.apache.catalina.startup.Bootstrap start");
        RoutingInfo routingInfo = LaunchedRouting.useLaunchingRouting("^com.alibaba.jvm.sandbox.*");
        assertNotNull(routingInfo);
        assertEquals(routingInfo.getType(), Type.TARGET_CLASS_LOADER_NAME);
        assertEquals(routingInfo.getTargetClassLoaderName(), "org.apache.catalina.loader.ParallelWebappClassLoader");
    }

    @Test
    public void testMatchingSpringBoot() {
        System.setProperty("sun.java.command", "org.springframework.boot.loader.JarLauncher start");
        RoutingInfo routingInfo = LaunchedRouting.useLaunchingRouting("^com.alibaba.jvm.sandbox.*");
        assertNotNull(routingInfo);
        assertEquals(routingInfo.getType(), Type.TARGET_CLASS_LOADER_NAME);
        assertEquals(routingInfo.getTargetClassLoaderName(), "org.springframework.boot.loader.LaunchedURLClassLoader");
    }

    @Test
    public void testMatchingPandoraBoot() {
        System.setProperty("sun.java.command", "com.taobao.pandora.boot.loader.SarLauncher start");
        RoutingInfo routingInfo = LaunchedRouting.useLaunchingRouting("^com.alibaba.jvm.sandbox.*");
        assertNotNull(routingInfo);
        assertEquals(routingInfo.getType(), Type.TARGET_CLASS_LOADER_NAME);
        assertEquals(routingInfo.getTargetClassLoaderName(), "com.taobao.pandora.boot.loader.LaunchedURLClassLoader");
    }
}