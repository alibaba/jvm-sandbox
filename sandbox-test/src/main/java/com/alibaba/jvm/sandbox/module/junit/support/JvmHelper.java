package com.alibaba.jvm.sandbox.module.junit.support;

import com.alibaba.jvm.sandbox.core.util.SpyUtils;
import org.apache.commons.lang3.StringUtils;

import static com.alibaba.jvm.sandbox.core.CoreConfigure.toConfigure;

/**
 * JVM帮助类，能模拟一个JVM对类的管理行为
 */
public class JvmHelper {

    private final String namespace;

    private JvmHelper(final String namespace) {
        this.namespace = namespace;
        SpyUtils.init(namespace);
        toConfigure(String.format(";namespace=%s;", namespace), "");
    }

    public static JvmHelper createJvm(final String namespace) {
        return new JvmHelper(StringUtils.isBlank(namespace) ? "default" : namespace);
    }

    public String getNamespace() {
        return namespace;
    }

}
