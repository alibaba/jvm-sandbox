package com.alibaba.jvm.sandbox.api.routing;

/**
 * {@link HttpRouting}
 * <p>
 *
 * @author zhaoyb1990
 */
public class HttpRouting extends AbstractRouting {

    @Override
    protected RoutingInfo getSecondary() {
        return RoutingInfo.withTargetClass("javax.servlet.http.HttpServlet", getPattern());
    }

    @Override
    protected String[] getPattern() {
        return new String[] {"^javax.servlet..*"};
    }
}
