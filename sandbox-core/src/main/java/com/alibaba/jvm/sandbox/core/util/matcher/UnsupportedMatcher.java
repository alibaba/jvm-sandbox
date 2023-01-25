package com.alibaba.jvm.sandbox.core.util.matcher;

import com.alibaba.jvm.sandbox.api.annotation.Stealth;
import com.alibaba.jvm.sandbox.core.manager.impl.SandboxClassFileTransformer;
import com.alibaba.jvm.sandbox.core.util.CoreStringUtils;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.Access;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;

import java.util.List;


/**
 * 不支持的类匹配
 *
 * @author luanjia@taobao.com
 */
public class UnsupportedMatcher implements Matcher {

    private final ClassLoader loader;
    private final boolean isEnableUnsafe;
    private final boolean isNativeSupported;

    public UnsupportedMatcher(final ClassLoader loader,
                              final boolean isEnableUnsafe,
                              final boolean isNativeSupported) {
        this.loader = loader;
        this.isEnableUnsafe = isEnableUnsafe;
        this.isNativeSupported = isNativeSupported;
    }

    // 是否因sandbox容器本身缺陷所暂时无法支持的类
    private boolean isUnsupportedClass(final ClassStructure classStructure) {
        return CoreStringUtils.containsAny(
                classStructure.getJavaClassName(),

                /*
                 * Lambda的方法拦截是一个深坑，常规则做法是拦截LambdaMetaFactory，
                 * 但这种做法相当不干净，而且无法实现ATTACH模式，所以这里选择性的放弃了对Lambda表达式的支持
                 */
                "$$Lambda$"
        );
    }

    // 是否已知的常用非必要增强类，常见的如CGLIB、Spring增强的类
    private boolean isNotNecessaryClass(final ClassStructure classStructure) {
        return CoreStringUtils.containsAny(
                classStructure.getJavaClassName(),
                "$$FastClassBySpringCGLIB$$",
                "$$EnhancerBySpringCGLIB$$",
                "$$EnhancerByCGLIB$$",
                "$$FastClassByCGLIB$$"
        );
    }

    /*
     * 是否是sandbox容器本身的类
     * 因为多命名空间的原因，所以这里不能简单的用ClassLoader来进行判断
     */
    private boolean isJvmSandboxClass(final ClassStructure classStructure) {
        return classStructure.getJavaClassName().startsWith("com.alibaba.jvm.sandbox.");
    }

    /*
     * 判断是否ClassLoader家族中是否有隐形基因
     */
    private boolean isFromStealthClassLoader() {
        if (null == loader) {
            return !isEnableUnsafe;
        }
        // FIX 292
        return loader.getClass().isAnnotationPresent(Stealth.class);
    }

    /*
     * 是否是负责启动的main函数
     * 这个函数如果被增强了会引起错误,所以千万不能增强,嗯嗯
     * public static void main(String[]);
     */
    private boolean isJavaMainBehavior(final BehaviorStructure behaviorStructure) {
        final Access access = behaviorStructure.getAccess();
        final List<ClassStructure> parameterTypeClassStructures = behaviorStructure.getParameterTypeClassStructures();
        return access.isPublic()
                && access.isStatic()
                && "void".equals(behaviorStructure.getReturnTypeClassStructure().getJavaClassName())
                && "main".equals(behaviorStructure.getName())
                && parameterTypeClassStructures.size() == 1
                && "java.lang.String[]".equals(parameterTypeClassStructures.get(0).getJavaClassName());
    }

    /*
     * 是否不支持的方法修饰
     */
    private boolean isUnsupportedBehavior(final BehaviorStructure behaviorStructure) {

        // 不支持抽象方法
        final Access access = behaviorStructure.getAccess();
        if (access.isAbstract()) {
            return true;
        }

        // 在JVM不允许native方法重定义的时候，不支持native方法
        return access.isNative() && !isNativeSupported;
    }

    /*
     * 不支持被SANDBOX修改的方法
     */
    private boolean isSandboxSpecialBehavior(final BehaviorStructure behaviorStructure) {
        return null != behaviorStructure.getName()
                && behaviorStructure.getName().startsWith(SandboxClassFileTransformer.SANDBOX_SPECIAL_PREFIX);
    }

    @Override
    public MatchingResult matching(final ClassStructure classStructure) {
        final MatchingResult result = new MatchingResult();
        if (isUnsupportedClass(classStructure)
                || isNotNecessaryClass(classStructure)
                || isJvmSandboxClass(classStructure)
                || isFromStealthClassLoader()
        ) {
            return result;
        }
        for (final BehaviorStructure behaviorStructure : classStructure.getBehaviorStructures()) {
            if (isJavaMainBehavior(behaviorStructure)
                    || isUnsupportedBehavior(behaviorStructure)
                    || isSandboxSpecialBehavior(behaviorStructure)) {
                continue;
            }
            result.getBehaviorStructures().add(behaviorStructure);
        }
        return result;
    }


    /**
     * 构造AND关系的组匹配
     * <p>
     * 一般{@link UnsupportedMatcher}都与其他Matcher配合使用，
     * 所以这里对AND关系做了一层封装
     * </p>
     *
     * @param matcher 发生AND关系的{@link Matcher}
     * @return GroupMatcher.and(matcher, this)
     */
    public Matcher and(final Matcher matcher) {
        return new GroupMatcher.And(
                matcher,
                this
        );
    }

}
