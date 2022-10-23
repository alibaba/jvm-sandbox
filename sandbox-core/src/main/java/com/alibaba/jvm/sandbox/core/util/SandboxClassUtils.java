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
     * 1. {@code com.alibaba.jvm.sandbox.}开头的类名
     * 2. 被{@code com.alibaba.jvm.sandbox.}开头的ClassLoader所加载的类
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
        return null != loader
                // fix issue #267
                && isSandboxPrefix(normalizeClass(loader.getClass().getName()));

    }

    /**
     * 标准化类名
     * <p>
     * 入参：com.alibaba.jvm.sandbox
     * 返回：com/alibaba/jvm/sandbox
     * </p>
     *
     * @param className 类名
     * @return 标准化类名
     */
    private static String normalizeClass(String className) {
        // #302
        return className.replace('.', '/');
    }

    /**
     * 是否是sandbox自身的类
     * <p>
     * 需要注意internalClassName的格式形如: com/alibaba/jvm/sandbox
     *
     * @param internalClassName 类资源名
     * @return true / false
     */
    private static boolean isSandboxPrefix(String internalClassName) {
        return internalClassName.startsWith(SANDBOX_FAMILY_CLASS_RES_PREFIX)
                && !isQaTestPrefix(internalClassName);
    }

    private static boolean isQaTestPrefix(String internalClassName) {
        return internalClassName.startsWith(SANDBOX_FAMILY_CLASS_RES_QATEST_PREFIX);
    }

}
