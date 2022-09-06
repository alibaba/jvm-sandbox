package com.alibaba.jvm.sandbox.api.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link LaunchedRouting}
 * <p>
 * 根据应用启动方式，匹配对应的容器的类加载器进行路由
 *
 * @author zhaoyb1990
 */
public class LaunchedRouting {

    private final static Pattern CLASS_PATTERN = Pattern.compile("\\S*([_a-zA-Z0-9.*])\\S*");

    private final static Map<String, String> CLASSLOADER_WRAPPER = new HashMap<String, String>();

    static {
        /* tomcat */
        CLASSLOADER_WRAPPER.put("org.apache.catalina.startup.Bootstrap",
                "org.apache.catalina.loader.ParallelWebappClassLoader");
        /* springboot */
        CLASSLOADER_WRAPPER.put("org.springframework.boot.loader.JarLauncher",
                "org.springframework.boot.loader.LaunchedURLClassLoader");
        /* pandora boot*/
        CLASSLOADER_WRAPPER.put("com.taobao.pandora.boot.loader.SarLauncher",
                "com.taobao.pandora.boot.loader.LaunchedURLClassLoader");
    }

    /**
     * 获取sun.java.command系统属性
     * |
     * v
     * 找到启动类
     * |
     * v
     * 映射classloader
     *
     * @param pattern 类匹配表达式
     * @return 类路由规则
     */
    public static RoutingInfo useLaunchingRouting(String... pattern) {
        String sunJavaCommand = System.getProperty("sun.java.command");
        String launchClass = matching(sunJavaCommand);
        if (isEmpty(launchClass)) {
            return null;
        }
        String classLoaderName = CLASSLOADER_WRAPPER.get(launchClass);
        if (isEmpty(classLoaderName)) {
            return null;
        }
        return RoutingInfo.withTargetClassloaderName(classLoaderName, pattern);
    }

    private static String matching(String source) {
        Matcher matcher = CLASS_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    private static boolean isEmpty(String sequence) {
        return sequence == null || sequence.isEmpty();
    }
}
