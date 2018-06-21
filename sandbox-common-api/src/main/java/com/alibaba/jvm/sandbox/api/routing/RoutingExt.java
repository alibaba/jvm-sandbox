package com.alibaba.jvm.sandbox.api.routing;

/**
 * <p>
 * 提供给模块的扩展类路由白名单spi
 *
 * @author yuebing.zyb@alibaba-inc.com 2018/4/24 10:38.
 */
public interface RoutingExt {

    /**
     * 获取类的特殊路由方式
     *
     * @return 类路由方式
     */
    RoutingInfo getSpecialRouting();
}
