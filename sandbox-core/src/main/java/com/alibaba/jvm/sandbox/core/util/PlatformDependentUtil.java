package com.alibaba.jvm.sandbox.core.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhuangpeng
 * @since 2020/12/3
 */
public class PlatformDependentUtil {

    private static final Logger logger = LoggerFactory.getLogger(PlatformDependentUtil.class);

    private static final int JAVA_VERSION = javaVersion0();

    public static int javaVersion() {
        return JAVA_VERSION;
    }

    private static int javaVersion0() {
        final int majorVersion;

        majorVersion = majorVersionFromJavaSpecificationVersion();
        logger.info("Java version: {}", majorVersion);

        return majorVersion;
    }

    // Package-private for testing only
    static int majorVersionFromJavaSpecificationVersion() {
        return majorVersion(get("java.specification.version", "1.6"));
    }


    // Package-private for testing only
    static int majorVersion(final String javaSpecVersion) {
        final String[] components = javaSpecVersion.split("\\.");
        final int[] version = new int[components.length];
        for (int i = 0; i < components.length; i++) {
            version[i] = Integer.parseInt(components[i]);
        }

        if (version[0] == 1) {
            assert version[1] >= 6;
            return version[1];
        } else {
            return version[0];
        }
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static String get(final String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(key);
                    }
                });
            }
        } catch (SecurityException e) {
            logger.warn("Unable to retrieve a system property '{}'; default values will be used.", key, e);
        }

        if (value == null) {
            return def;
        }

        return value;
    }

}
