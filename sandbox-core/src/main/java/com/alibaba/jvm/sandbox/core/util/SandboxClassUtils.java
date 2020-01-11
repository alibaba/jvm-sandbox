package com.alibaba.jvm.sandbox.core.util;

/**
 * Sandbox的类操作工具类
 */
public class SandboxClassUtils {

    private static final String SANDBOX_FAMILY_CLASS_RES_PREFIX = "com/alibaba/jvm/sandbox/";
    private static final String SANDBOX_FAMILY_CLASS_RES_QATEST_PREFIX = "com/alibaba/jvm/sandbox/qatest";

    /**
     * 是否是SANDBOX家族所管理的类
     * <p>
     * SANDBOX家族所管理的类包括：
     * <li>{@code com.alibaba.jvm.sandbox.}开头的类名</li>
     * <li>被{@code com.alibaba.jvm.sandbox.}开头的ClassLoader所加载的类</li>
     * </p>
     *
     * @param internalClassName 类资源名
     * @param loader            加载类的ClassLoader
     * @return true:属于SANDBOX家族;false:不属于
     */
    public static boolean isComeFromSandboxFamily(final String internalClassName, final ClassLoader loader) {

        // 类名是com.alibaba.jvm.sandbox开头
        if (null != internalClassName
                && isSandboxPrefix(internalClassName)) {
            return true;
        }

        // 类被com.alibaba.jvm.sandbox开头的ClassLoader所加载
        if (null != loader
                && isSandboxPrefix(loader.getClass().getName())) {
            return true;
        }

        return false;

    }

    private static boolean isSandboxPrefix(String internalClassName) {
        return internalClassName.startsWith(SANDBOX_FAMILY_CLASS_RES_PREFIX)
                && !isQaTestPrefix(internalClassName);
    }

    private static boolean isQaTestPrefix(String internalClassName) {
        return internalClassName.startsWith(SANDBOX_FAMILY_CLASS_RES_QATEST_PREFIX);
    }

}
