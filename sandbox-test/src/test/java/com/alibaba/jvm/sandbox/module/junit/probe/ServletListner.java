package com.alibaba.jvm.sandbox.module.junit.probe;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;

import java.util.Map;


/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020-10-13 12:17
 */
public class ServletListner extends AdviceListener {

    @Override
    protected void before(Advice advice) throws Throwable {

        if (!advice.isProcessTop()) {
            return;
        }

        // wrap httpServletRequest
        final IHttpServletRequest httpServletRequest = InterfaceProxyUtils.puppet(
                IHttpServletRequest.class,
                advice.getParameterArray()[0]
        );

        // init HttpAccess
        final HttpAccess httpAccess = new HttpAccess(
                httpServletRequest.getRemoteAddress(),
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                httpServletRequest.getParameterMap(),
                httpServletRequest.getHeader("User-Agent")
        );

        // attach
        advice.attach(httpAccess);

        String username = httpServletRequest.getHeader("username");
        if ("admin".equals(username)) {
            System.out.println("Admin login");
        } else {
            System.out.println("login user: " + username);
        }


    }

    private interface IHttpServletRequest {

        @InterfaceProxyUtils.ProxyMethod(name = "getRemoteAddr")
        String getRemoteAddress();

        String getMethod();

        String getRequestURI();

        Map<String, String[]> getParameterMap();

        String getHeader(String name);

    }

}
