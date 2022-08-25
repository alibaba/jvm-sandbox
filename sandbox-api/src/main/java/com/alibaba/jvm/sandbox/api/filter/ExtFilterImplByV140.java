package com.alibaba.jvm.sandbox.api.filter;

/**
 * 增强过滤器V140实现，根据#292实现
 *
 * @since {@code sandbox-api:1.4.0}
 */
public class ExtFilterImplByV140 implements ExtFilter {

    private final ExtFilter target;
    private final boolean isHasInterfaceTypes;
    private final boolean isHasAnnotationTypes;

    /**
     * 增强过滤器V140实现
     *
     * @param target               代理增强过滤器目标
     * @param isHasInterfaceTypes  是否需要过滤接口类型
     * @param isHasAnnotationTypes 是否需要过滤注解类型
     */
    public ExtFilterImplByV140(ExtFilter target, boolean isHasInterfaceTypes, boolean isHasAnnotationTypes) {
        this.target = target;
        this.isHasInterfaceTypes = isHasInterfaceTypes;
        this.isHasAnnotationTypes = isHasAnnotationTypes;
    }

    /**
     * 是否需要过滤接口类型
     *
     * @return TRUE | FALSE
     */
    public boolean isHasInterfaceTypes() {
        return isHasInterfaceTypes;
    }

    /**
     * 是否需要过滤注解类型
     *
     * @return TRUE | FALSE
     */
    public boolean isHasAnnotationTypes() {
        return isHasAnnotationTypes;
    }

    @Override
    public boolean isIncludeSubClasses() {
        return target.isIncludeSubClasses();
    }

    @Override
    public boolean isIncludeBootstrap() {
        return target.isIncludeBootstrap();
    }

    @Override
    public boolean doClassFilter(final int access,
                                 final String javaClassName,
                                 final String superClassTypeJavaClassName,
                                 final String[] interfaceTypeJavaClassNameArray,
                                 final String[] annotationTypeJavaClassNameArray) {
        return target.doClassFilter(
                access,
                javaClassName,
                superClassTypeJavaClassName,
                interfaceTypeJavaClassNameArray,
                annotationTypeJavaClassNameArray
        );
    }

    @Override
    public boolean doMethodFilter(final int access,
                                  final String javaMethodName,
                                  final String[] parameterTypeJavaClassNameArray,
                                  final String[] throwsTypeJavaClassNameArray,
                                  final String[] annotationTypeJavaClassNameArray) {
        return target.doMethodFilter(
                access,
                javaMethodName,
                parameterTypeJavaClassNameArray,
                throwsTypeJavaClassNameArray,
                annotationTypeJavaClassNameArray
        );
    }

}
