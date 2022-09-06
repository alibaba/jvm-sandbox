package com.alibaba.jvm.sandbox.core.jni;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.alibaba.jvm.sandbox.AnchorPoint;
import com.alibaba.jvm.sandbox.JniAnchorPoint;
import com.alibaba.jvm.sandbox.api.resource.JniAnchorManager;
import com.alibaba.jvm.sandbox.util.JniUtil;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DefaultJniAnchorManager}
 * <p>
 *
 * @author zhaoyb1990
 */
public class DefaultJniAnchorManager implements JniAnchorManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AnchorPoint target;

    /**
     * 初始化jni服务
     *
     * @param sandboxHome sandbox包绝对路径
     */
    public DefaultJniAnchorManager(String sandboxHome) {
        // 打包时将lib加载路径: {sandbox_home}/lib/jni/
        try {
            String library = JniUtil.chooseJniLib();
            // {sandbox_home}/lib/jni/{library}
            File soFile = new File(sandboxHome, "lib" + File.separator + "jni" + File.separator + library);
            if (!soFile.exists()) {
                throw new FileNotFoundException(soFile.getAbsolutePath());
            }
            FileOutputStream tmpLibOutputStream = null;
            FileInputStream libInputStream = null;
            try {
                File tmp = File.createTempFile(JniAnchorPoint.JNI_LIBRARY_NAME, null);
                tmpLibOutputStream = new FileOutputStream(tmp);
                libInputStream = new FileInputStream(soFile.getAbsolutePath());
                IOUtils.copy(libInputStream, tmpLibOutputStream);
                logger.info("Init Jni-AnchorPoint: Copy {} to {}", soFile.getAbsolutePath(), tmp.getAbsolutePath());
                target = JniAnchorPoint.getInstance(tmp.getAbsolutePath());
                logger.info("Init Jni-AnchorPoint Finish, target={}", target);
            } finally {
                IOUtils.closeQuietly(libInputStream);
                IOUtils.closeQuietly(tmpLibOutputStream);
            }
        } catch (Throwable throwable) {
            target = new AnchorPoint() {
                @Override
                public <T> T[] getInstances(Class<T> klass, int limit) {
                    throw new RuntimeException("un-support operation");
                }

                @Override
                public <T> T[] getInstances(Class<T> klass) {
                    throw new RuntimeException("un-support operation");
                }
            };
            logger.error("Init Jni-AnchorPoint occurred error, using fake proxy implement", throwable);
        }
    }

    @Override
    public <T> T[] getInstances(Class<T> klass, int limit) {
        return target.getInstances(klass, limit);
    }

    @Override
    public <T> T[] getInstances(Class<T> klass) {
        return target.getInstances(klass);
    }
}
