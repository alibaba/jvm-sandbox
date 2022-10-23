package com.alibaba.jvm.sandbox.api.routing;

/**
 * <p>
 * 提供给模块使用的目标类路由白名单，匹配到pattern的类将通过目标类加载器加载
 * <p>
 * - 模块可以通过引入业务包进行编码，不用通过反射调用（引入jar: scope=provided)
 * - 一些框架类型自定义类隔离机制（类似SOFA/Pandora）可自由选择类加载
 *
 * @author zhaoyb1990
 */
public class RoutingInfo {

    /**
     * 路由类型
     */
    public enum Type {
        /**
         * 被pattern匹配的类，由目标的类的类加载器进行加载
         */
        TARGET_CLASS,
        /**
         * 被pattern匹配的类，由目标类载器进行加载
         */
        TARGET_CLASS_LOADER,
        /**
         * 被pattern匹配的类，由目标类加载器进行加载
         */
        TARGET_CLASS_LOADER_NAME
    }

    /**
     * 路由匹配正则表达式
     */
    private String[] pattern;

    /**
     * 路由类型
     */
    private Type type;

    /**
     * 目标类
     */
    private String targetClass;

    /**
     * 目标类加载器的名字
     */
    private String targetClassLoaderName;

    /**
     * 目标类加载器
     */
    private ClassLoader targetClassloader;

    private RoutingInfo() {}

    /**
     * 使用目标类的加载器进行路由
     *
     * @param targetClass 目标类
     * @param pattern     匹配类正则
     */
    public RoutingInfo(String targetClass, String... pattern) {
        this.type = Type.TARGET_CLASS;
        this.pattern = pattern;
        this.targetClass = targetClass;
    }

    /**
     * 使用目标类的加载器进行路由
     *
     * @param classLoader 目标类加载器
     * @param pattern     匹配类正则
     */
    public RoutingInfo(ClassLoader classLoader, String... pattern) {
        this.type = Type.TARGET_CLASS_LOADER;
        this.pattern = pattern;
        this.targetClassloader = classLoader;
    }

    /**
     * 使用目标类名称进行路由
     *
     * @param targetClass 目标类名称（如果出现多个类加载器，会使用第一个，默认会屏蔽sandbox自身的类加载器）
     * @param pattern     匹配类正则
     * @return 路由规则
     */
    public static RoutingInfo withTargetClass(String targetClass, String... pattern) {
        RoutingInfo routingInfo = new RoutingInfo();
        routingInfo.type = Type.TARGET_CLASS;
        routingInfo.pattern = pattern;
        routingInfo.targetClass = targetClass;
        return routingInfo;
    }

    /**
     * 使用目标类加载器进行路由
     *
     * @param targetClassloader 目标加载器
     * @param pattern           匹配类正则
     * @return 路由规则
     */
    public static RoutingInfo withTargetClassloader(ClassLoader targetClassloader, String... pattern) {
        RoutingInfo routingInfo = new RoutingInfo();
        routingInfo.type = Type.TARGET_CLASS_LOADER;
        routingInfo.pattern = pattern;
        routingInfo.targetClassloader = targetClassloader;
        return routingInfo;
    }

    /**
     * 使用目标类路由器名称进行路由
     *
     * @param targetClassLoaderName 目标类路由器名称, 通过classLoader#getClass()#getName()提取
     * @param pattern               匹配类正则
     * @return 路由规则
     */
    public static RoutingInfo withTargetClassloaderName(String targetClassLoaderName, String... pattern) {
        RoutingInfo routingInfo = new RoutingInfo();
        routingInfo.type = Type.TARGET_CLASS_LOADER_NAME;
        routingInfo.pattern = pattern;
        routingInfo.targetClassLoaderName = targetClassLoaderName;
        return routingInfo;
    }

    public String[] getPattern() {
        return pattern;
    }

    public Type getType() {
        return type;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public ClassLoader getTargetClassloader() {
        return targetClassloader;
    }

    public String getTargetClassLoaderName() {
        return targetClassLoaderName;
    }
}
