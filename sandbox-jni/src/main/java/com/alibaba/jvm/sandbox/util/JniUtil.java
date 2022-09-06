package com.alibaba.jvm.sandbox.util;

/**
 * {@link JniUtil}
 * <p>
 *
 * @author zhaoyb1990
 */
public class JniUtil {

    private final static String MAC_JNI_LIB = "libSandboxJniLibrary-darwin.dylib";

    private final static String LINUX_JNI_LIB = "libSandboxJniLibrary-linux.so";

    private final static String WINDOWS_JNI_LIB = "libSandboxJniLibrary-win32.dll";

    /**
     * 根据当前系统选择jni-lib的包
     *
     * @return jni-lib
     */
    public static String chooseJniLib() {

        // linux
        if (OsUtils.isLinux()) {
            return LINUX_JNI_LIB;
        }

        // mac
        if (OsUtils.isMac()) {
            return MAC_JNI_LIB;
        }

        // windows
        if (OsUtils.isWindows()) {
            return WINDOWS_JNI_LIB;
        }

        throw new RuntimeException("Un support platform detected, no jni library can match this platform.");
    }
}
