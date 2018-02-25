package com.alibaba.jvm.sandbox.core.util.matcher;

import com.alibaba.jvm.sandbox.api.annotation.IncludeBootstrap;
import com.alibaba.jvm.sandbox.api.annotation.IncludeSubClasses;
import com.alibaba.jvm.sandbox.api.filter.AccessFlags;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.Access;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.alibaba.jvm.sandbox.api.filter.AccessFlags.*;

/**
 * 过滤器实现的匹配器
 *
 * @author luanjia@taobao.com
 */
public class FilterMatcher implements Matcher {

    private final Filter filter;
    private final boolean isIncludeSubClass;
    private final boolean isIncludeBootstrap;

    public FilterMatcher(final Filter filter) {
        this.filter = filter;
        this.isIncludeSubClass = filter.getClass().isAnnotationPresent(IncludeSubClasses.class);
        this.isIncludeBootstrap = filter.getClass().isAnnotationPresent(IncludeBootstrap.class);
    }

    // 获取需要匹配的类结构
    // 如果要匹配子类就需要将这个类的所有家族成员找出
    private Collection<ClassStructure> getWaitingMatchClassStructures(final ClassStructure classStructure) {
        final Collection<ClassStructure> waitingMatchClassStructures = new ArrayList<ClassStructure>();
        waitingMatchClassStructures.add(classStructure);
        if (isIncludeSubClass) {
            waitingMatchClassStructures.addAll(classStructure.getFamilyTypeClassStructures());
        }
        return waitingMatchClassStructures;
    }

    private String[] toJavaClassNameArray(final Collection<ClassStructure> classStructures) {
        if (null == classStructures) {
            return null;
        }
        final Set<String> javaClassNames = new LinkedHashSet<String>();
        for (final ClassStructure classStructure : classStructures) {
            javaClassNames.add(classStructure.getJavaClassName());
        }
        return javaClassNames.toArray(new String[0]);
    }

    private boolean matchingClassStructure(ClassStructure classStructure) {
        for (final ClassStructure wmCs : getWaitingMatchClassStructures(classStructure)) {

            // 匹配类结构
            if (filter.doClassFilter(
                    toFilterAccess(wmCs.getAccess()),
                    wmCs.getJavaClassName(),
                    null == wmCs.getSuperClassStructure()
                            ? null
                            : wmCs.getSuperClassStructure().getJavaClassName(),
                    toJavaClassNameArray(wmCs.getFamilyInterfaceClassStructures()),
                    toJavaClassNameArray(wmCs.getFamilyAnnotationTypeClassStructures())
            )) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MatchingResult matching(final ClassStructure classStructure) {
        final MatchingResult result = new MatchingResult();

        // 1. 匹配ClassStructure
        if (!matchingClassStructure(classStructure)) {
            return result;
        }

        // 如果不开启加载Bootstrap的类，遇到就过滤掉
        if (!isIncludeBootstrap
                && classStructure.getClassLoader() == null) {
            return result;
        }

        // 2. 匹配BehaviorStructure
        for (final BehaviorStructure behaviorStructure : classStructure.getBehaviorStructures()) {
            if (filter.doMethodFilter(
                    toFilterAccess(behaviorStructure.getAccess()),
                    behaviorStructure.getName(),
                    toJavaClassNameArray(behaviorStructure.getParameterTypeClassStructures()),
                    toJavaClassNameArray(behaviorStructure.getExceptionTypeClassStructures()),
                    toJavaClassNameArray(behaviorStructure.getAnnotationTypeClassStructures())
            )) {
                result.getBehaviorStructures().add(behaviorStructure);
            }
        }

        return result;
    }

    /**
     * 转换为{@link AccessFlags}的Access体系
     *
     * @param access access flag
     * @return 部分兼容ASM的access flag
     */
    private static int toFilterAccess(final Access access) {
        int flag = 0;
        if (access.isPublic()) flag |= ACF_PUBLIC;
        if (access.isPrivate()) flag |= ACF_PRIVATE;
        if (access.isProtected()) flag |= ACF_PROTECTED;
        if (access.isStatic()) flag |= ACF_STATIC;
        if (access.isFinal()) flag |= ACF_FINAL;
        if (access.isInterface()) flag |= ACF_INTERFACE;
        if (access.isNative()) flag |= ACF_NATIVE;
        if (access.isAbstract()) flag |= ACF_ABSTRACT;
        if (access.isEnum()) flag |= ACF_ENUM;
        if (access.isAnnotation()) flag |= ACF_ANNOTATION;
        return flag;
    }

}
