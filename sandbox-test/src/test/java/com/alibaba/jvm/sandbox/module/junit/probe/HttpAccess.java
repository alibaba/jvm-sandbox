package com.alibaba.jvm.sandbox.module.junit.probe;

import java.util.Map;

public class HttpAccess {

    private final String from;
    private final String method;
    private final String uri;
    private final Map<String, String[]> parameterMap;
    private final String userAgent;
    private int status = 200;

    public HttpAccess(final String from,
                      final String method,
                      final String uri,
                      final Map<String, String[]> parameterMap,
                      final String userAgent) {
        this.from = from;
        this.method = method;
        this.uri = uri;
        this.parameterMap = parameterMap;
        this.userAgent = userAgent;
    }

    public String getFrom() {
        return from;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
