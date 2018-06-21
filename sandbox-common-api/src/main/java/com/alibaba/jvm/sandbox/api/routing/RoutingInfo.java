package com.alibaba.jvm.sandbox.api.routing;

/**
 * <p>
 * 提供给模块使用的目标类路由白名单，白名单的类不隔离
 *
 * 模块可以通过引入业务包进行编码
 *
 * 使用类的时候，调用业务的类加载器，保证模块可以正常使用业务系统中的类
 *
 * @author yuebing.zyb@alibaba-inc.com 2018/4/24 10:38.
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
        TARGET_CLASS_LOADER
    }

    /**
     * 路由匹配正则表达式
     */
    private String[] pattern;

    /**
     * 路由类型
     */
    private RoutingInfo.Type type;

    /**
     * 目标类
     */
    private String targetClass;

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
     * @param classLoader
     * @param pattern
     */
    public RoutingInfo(ClassLoader classLoader, String... pattern) {
        this.type = Type.TARGET_CLASS_LOADER;
        this.pattern = pattern;
        this.targetClassloader = classLoader;
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
}
