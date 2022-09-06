package com.alibaba.jvm.sandbox.util;

import java.util.Locale;

/**
 * {@link OsUtils}
 * <p>
 * 操作系统识别工具类
 *
 * @author zhaoyb1990
 */
public class OsUtils {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    /**
     * 系统架构 - 暂未使用，对于win32/64位，以及一些arm架构差异性特殊处理需要
     */
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    private static final OperationSystem OPERATION_SYSTEM = OperationSystem.judge(OS_NAME);

    public static boolean isMac() {
        return OperationSystem.MAC.equals(OPERATION_SYSTEM);
    }

    public static boolean isLinux() {
        return OperationSystem.LINUX.equals(OPERATION_SYSTEM);
    }

    public static boolean isWindows() {
        return OperationSystem.WINDOWS.equals(OPERATION_SYSTEM);
    }

    public enum OperationSystem {

        /**
         * MAC-OS
         */
        MAC(new String[] {"mac", "darwin"}),
        /**
         * linux
         */
        LINUX(new String[] {"linux"}),
        /**
         * windows
         */
        WINDOWS(new String[] {"windows"}),
        /**
         * other
         */
        OTHER(new String[] {"other"});

        private final String[] keywords;

        OperationSystem(String[] keywords) {
            this.keywords = keywords;
        }

        public String[] getKeywords() {
            return keywords;
        }

        public static OperationSystem judge(String osName) {
            for (OperationSystem os : values()) {
                for (String keyword : os.keywords) {
                    if (osName.startsWith(keyword)) {
                        return os;
                    }
                }
            }
            return OTHER;
        }
    }
}
