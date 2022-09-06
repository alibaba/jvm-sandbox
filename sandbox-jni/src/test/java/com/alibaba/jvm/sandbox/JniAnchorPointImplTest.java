package com.alibaba.jvm.sandbox;

import java.io.File;

import junit.framework.TestCase;
import org.junit.Assert;

import com.alibaba.jvm.sandbox.util.JniUtil;

/**
 * {@link JniAnchorPointImplTest}
 * <p>
 *
 * @author zhaoyb1990
 */
public class JniAnchorPointImplTest extends TestCase {

    private JniAnchorPoint initJniAnchorPoint() {
        File path = new File(JniAnchorPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        String libPath = new File(path, JniUtil.chooseJniLib()).getAbsolutePath();
        return JniAnchorPoint.getInstance(libPath);
    }

    public void testGetInstance() {
        JniAnchorPoint jniAnchorPointImpl = initJniAnchorPoint();
        JniAnchorPointImplTest[] instances = jniAnchorPointImpl.getInstances(JniAnchorPointImplTest.class);
        Assert.assertNotNull(instances);
    }
}