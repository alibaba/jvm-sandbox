package com.alibaba.jvm.sandbox.core.classloader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import com.alibaba.jvm.sandbox.api.routing.RoutingExt;
import com.alibaba.jvm.sandbox.api.routing.RoutingInfo;
import com.alibaba.jvm.sandbox.api.routing.RoutingInfo.Type;
import com.alibaba.jvm.sandbox.core.classloader.SpecialRoutingHandler.RoutingYaml;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link SpecialRoutingHandlerTest}
 * <p>
 *
 * @author zhaoyb1990
 */
public class SpecialRoutingHandlerTest {

    private final static Pattern API_JAR_FILE_PATTERN = Pattern.compile("sandbox-api-[\\w.+]*.jar");

    File jarFile = null;

    @Before
    public void before() {
        File path = new File(
                RoutingExt.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        assertNotNull(path);
        for (File file : path.listFiles()) {
            if (API_JAR_FILE_PATTERN.matcher(file.getName()).matches()) {
                jarFile = file;
                break;
            }
        }
        assertNotNull(jarFile);
    }

    @Test
    public void testResolve() throws IOException {
        List<RoutingInfo> routingInfos = SpecialRoutingHandler.resolve(jarFile);
        assertNotNull(routingInfos);
        assertEquals(3, routingInfos.size());

        assertEquals(Type.TARGET_CLASS, routingInfos.get(0).getType());
        assertEquals("org.apache.dubbo.rpc.model.ApplicationModel", routingInfos.get(0).getTargetClass());
        assertEquals(1, routingInfos.get(0).getPattern().length);
        assertEquals("^org.apache.dubbo..*", routingInfos.get(0).getPattern()[0]);

        assertEquals(Type.TARGET_CLASS, routingInfos.get(1).getType());
        assertEquals("javax.servlet.http.HttpServlet", routingInfos.get(1).getTargetClass());
        assertEquals(1, routingInfos.get(1).getPattern().length);
        assertEquals("^javax.servlet..*", routingInfos.get(1).getPattern()[0]);

        assertEquals(Type.TARGET_CLASS_LOADER_NAME, routingInfos.get(2).getType());
        assertEquals("org.apache.catalina.loader.WebappClassLoader", routingInfos.get(2).getTargetClassLoaderName());
        assertEquals(1, routingInfos.get(2).getPattern().length);
        assertEquals("^org.apache.dubbo..*", routingInfos.get(2).getPattern()[0]);

    }

    @Test
    public void testGetRoutingYamlConfig() throws IOException {

        RoutingYaml yaml = SpecialRoutingHandler.getRoutingYamlConfig(jarFile);
        assertNotNull(yaml);
        assertTrue(yaml.isClassRoutingEnable());
        assertEquals("yaml", yaml.getRoutingConfigType());
        assertEquals(3, yaml.getRoutingConfigs().size());

        assertTrue(yaml.getRoutingConfigs().get(0).isUsingApp());
        assertEquals("targetClass", yaml.getRoutingConfigs().get(0).getType());
        assertEquals("org.apache.dubbo.rpc.model.ApplicationModel", yaml.getRoutingConfigs().get(0).getTargetName());
        assertEquals(1, yaml.getRoutingConfigs().get(0).getPattern().size());
        assertEquals("^org.apache.dubbo..*", yaml.getRoutingConfigs().get(0).getPattern().get(0));

        assertTrue(yaml.getRoutingConfigs().get(1).isUsingApp());
        assertEquals("targetClass", yaml.getRoutingConfigs().get(1).getType());
        assertEquals("javax.servlet.http.HttpServlet", yaml.getRoutingConfigs().get(1).getTargetName());
        assertEquals(1, yaml.getRoutingConfigs().get(1).getPattern().size());
        assertEquals("^javax.servlet..*", yaml.getRoutingConfigs().get(1).getPattern().get(0));

        assertFalse(yaml.getRoutingConfigs().get(2).isUsingApp());
        assertEquals("targetClassloaderName", yaml.getRoutingConfigs().get(2).getType());
        assertEquals("org.apache.catalina.loader.WebappClassLoader", yaml.getRoutingConfigs().get(2).getTargetName());
        assertEquals(1, yaml.getRoutingConfigs().get(2).getPattern().size());
        assertEquals("^org.apache.dubbo..*", yaml.getRoutingConfigs().get(2).getPattern().get(0));

    }
}