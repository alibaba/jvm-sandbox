package com.alibaba.jvm.sandbox;

/**
 * {@link JniAnchorPoint}
 * <p>
 *
 * @author zhaoyb1990
 */
public class JniAnchorPoint implements AnchorPoint {

    /**
     * jni-lib
     */
    public final static String JNI_LIBRARY_NAME = "libSandboxJniLibrary";

    private static String libRealPath = JNI_LIBRARY_NAME;

    /**
     * 获取klass在当前jvm中的实例对象
     *
     * @param klass 目标类型
     * @param limit 返回条数
     * @param <T>   泛型
     * @return 实例列表
     */
    private static synchronized native <T> T[] getInstances0(Class<T> klass, int limit);

    private static JniAnchorPoint INSTANCE = null;

    private JniAnchorPoint() {}

    /**
     * 获取标准lib路径实例
     *
     * @return jni实例
     */
    public static JniAnchorPoint getInstance() {
        return getInstance(null);
    }

    /**
     * 获取特定lib路径下的实例
     *
     * @param libSpecialPath jni-lib路径
     * @return jni实例
     */
    public static JniAnchorPoint getInstance(String libSpecialPath) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        if (libSpecialPath == null) {
            System.loadLibrary(JNI_LIBRARY_NAME);
        } else {
            libRealPath = libSpecialPath;
            System.load(libSpecialPath);
        }
        INSTANCE = new JniAnchorPoint();
        return INSTANCE;
    }

    @Override
    public <T> T[] getInstances(Class<T> klass, int limit) {
        return getInstances0(klass, limit);
    }

    @Override
    public <T> T[] getInstances(Class<T> klass) {
        return getInstances0(klass, -1);
    }

    @Override
    public String toString() {
        return "JniAnchorPoint(" + libRealPath + ")";
    }
}
