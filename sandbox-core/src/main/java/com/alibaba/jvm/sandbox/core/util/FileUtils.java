package com.alibaba.jvm.sandbox.core.util;

import java.io.File;
import java.io.IOException;

/**
 * 文件工具类
 *
 * @author zhimeng.zm@outlook.com
 */
public class FileUtils {
    private static final String TMP_DIR_KEY = "java.io.tmpdir";

    public static void makeTmpDirIfNeeded() throws IOException {
        File tmpDir = new File(System.getProperty(TMP_DIR_KEY));
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }

}
