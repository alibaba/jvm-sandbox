package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.util.SandboxStringUtils;

/**
 * 名称模版匹配过滤器
 *
 * @author launajia@taobao.com
 */
public class NamePatternFilter implements Filter {

    private final String classNamePattern;
    private final String methodNamePattern;

    public NamePatternFilter(String classNamePattern, String methodNamePattern) {
        this.classNamePattern = classNamePattern;
        this.methodNamePattern = methodNamePattern;
    }

    @Override
    public boolean doClassFilter(final int access,
                                 final String javaClassName,
                                 final String superClassTypeJavaClassName,
                                 final String[] interfaceTypeJavaClassNameArray,
                                 final String[] annotationTypeJavaClassNameArray) {
        return SandboxStringUtils.matching(javaClassName, classNamePattern);
    }

    @Override
    public boolean doMethodFilter(final int access,
                                  final String javaMethodName,
                                  final String[] parameterTypeJavaClassNameArray,
                                  final String[] throwsTypeJavaClassNameArray,
                                  final String[] annotationTypeJavaClassNameArray) {
        return SandboxStringUtils.matching(javaMethodName, methodNamePattern);
    }

}
