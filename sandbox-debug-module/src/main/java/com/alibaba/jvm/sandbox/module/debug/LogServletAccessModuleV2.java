package com.alibaba.jvm.sandbox.module.debug;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;

import org.kohsuke.MetaInfServices;

/**
 * {@link LogServletAccessModuleV2}
 * <p>
 * 基于HTTP-SERVLET(v2.4)规范的HTTP访问日志
 * <p>
 * {@link LogServletAccessModule}的重构，利用类路由模式简化调用流程
 *
 * @author zhaoyb1990
 */
@MetaInfServices(Module.class)
@Information(id = "debug-servlet-access-v2", version = "0.0.1", author = "zhaoyb1990@foxmail.com")
public class LogServletAccessModuleV2 extends LogServletAccessModule {

    /**
     * 新的包装方式，通过{@link com.alibaba.jvm.sandbox.api.routing.RoutingExt} 进行类路由
     * 将{@link HttpServletRequest}类交由业务自身的类加载器，在模块中可以直接使用类和方法
     *
     * @param advice 事件行为通知
     * @return 包装HttpAccess
     */
    @Override
    protected HttpAccess wrapperHttpAccess(Advice advice) {

        // 配置类自定义路由后，模块编码可以直接强转，不必再通过反射调用
        // 如果目标进程中没有HttpServletRequest，这里会直接抛异常
        HttpServletRequest httpServletRequest = (HttpServletRequest)advice.getParameterArray()[0];

        final HttpAccess httpAccess = new HttpAccess(
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                httpServletRequest.getParameterMap(),
                httpServletRequest.getHeader("User-Agent")
        );

        return httpAccess;
    }
}
