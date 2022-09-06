package com.alibaba.jvm.sandbox.api.routing;

/**
 * {@link AbstractRouting}
 * <p>
 * 抽象路由规则实现；包装了一层{@link AbstractRouting#appRouting()}逻辑，帮助找到应用的业务类加载器并优先使用
 *
 * @author zhaoyb1990
 */
public abstract class AbstractRouting implements RoutingExt {

    @Override
    public RoutingInfo getSpecialRouting() {
        // 优先根据启动方式使用业务类加载器路由
        RoutingInfo appRouting = appRouting();
        if (appRouting != null) {
            return appRouting;
        }
        return getSecondary();
    }

    /**
     * 应用类加载器
     *
     * @return 类路由器
     */
    protected RoutingInfo appRouting() {
        return LaunchedRouting.useLaunchingRouting(getPattern());
    }

    /**
     * 兜底路由信息
     *
     * @return 类路由器
     */
    protected abstract RoutingInfo getSecondary();

    /**
     * 类路由的匹配器
     *
     * @return 匹配表达式
     */
    protected abstract String[] getPattern();
}
