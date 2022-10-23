package com.alibaba.jvm.sandbox.api.routing;

/**
 * <p>
 * 提供给模块的扩展类路由SPI
 * <p>
 * 使用该扩展能力需要在模块资源路径(src/main/resources)下创建yaml配置文件(META-INFO/class-routing-config.yaml)，开启路由开关并配置SPI模式
 * <p>
 * 详细配置方式，可参考(META-INFO/class-routing-config.yaml)
 *
 * @author zhaoyb1990
 * @since {@code sandbox-common-api:1.4.0}
 */
public interface RoutingExt {

    /**
     * 获取类的特殊路由方式
     *
     * @return 类路由方式
     */
    RoutingInfo getSpecialRouting();
}
