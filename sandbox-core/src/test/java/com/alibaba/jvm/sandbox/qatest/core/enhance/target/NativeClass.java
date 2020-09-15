package com.alibaba.jvm.sandbox.qatest.core.enhance.target;

import java.util.Properties;

/**
 * @author zhuangpeng
 * @since 2020/9/13
 */
public class NativeClass {

    public static native long currentTimeMillis();

    public static native Properties param1(Properties prop);

    public static native String param2(String a, Object b);

    public native Properties param3(Properties prop);

    public native String param4(String a, Object b);
}
