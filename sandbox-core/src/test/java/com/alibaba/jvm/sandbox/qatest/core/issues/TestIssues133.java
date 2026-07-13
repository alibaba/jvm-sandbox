package com.alibaba.jvm.sandbox.qatest.core.issues;

import org.junit.Assert;
import org.junit.Test;

import static com.alibaba.jvm.sandbox.core.util.AsmUtils.getCommonSuperClass;

public class TestIssues133 {

    @Test
    public void test() {
        final ClassLoader loader = getClass().getClassLoader();
        Assert.assertEquals("java/io/InputStream", getCommonSuperClass("java/io/FileInputStream", "jakarta/servlet/ServletInputStream", loader));
        Assert.assertEquals("java/lang/Exception", getCommonSuperClass("java/io/IOException", "jakarta/servlet/ServletException", loader));
        Assert.assertEquals("jakarta/servlet/ServletResponse", getCommonSuperClass("jakarta/servlet/ServletResponse", "jakarta/servlet/http/HttpServletResponse", loader));
        Assert.assertEquals("jakarta/servlet/ServletResponse", getCommonSuperClass("jakarta/servlet/http/HttpServletResponse", "jakarta/servlet/ServletResponse", loader));
        Assert.assertEquals("java/lang/Object", getCommonSuperClass("java/lang/Throwable", "java/io/FileInputStream", loader));
        Assert.assertEquals("java/lang/Exception", getCommonSuperClass("java/io/IOException", "java/lang/Exception", null));
        Assert.assertEquals("java/lang/Exception", getCommonSuperClass("java/io/IOException", "jakarta/servlet/ServletException", null));
        Assert.assertEquals("java/lang/Object", getCommonSuperClass("java/lang/Throwable", "java/io/FileInputStream", null));
        Assert.assertEquals("jakarta/servlet/ServletResponse", getCommonSuperClass("jakarta/servlet/ServletResponse", "jakarta/servlet/http/HttpServletResponse", null));
        Assert.assertEquals("jakarta/servlet/ServletResponse", getCommonSuperClass("jakarta/servlet/http/HttpServletResponse", "jakarta/servlet/ServletResponse", null));
    }

}
