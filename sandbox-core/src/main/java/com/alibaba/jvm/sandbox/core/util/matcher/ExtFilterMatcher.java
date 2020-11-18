package com.alibaba.jvm.sandbox.core.util.matcher;

import com.alibaba.jvm.sandbox.api.filter.ExtFilter;
import com.alibaba.jvm.sandbox.api.filter.ExtFilter.ExtFilterFactory;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.core.util.matcher.inner.BehaviorExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.inner.BehaviorMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.inner.ClassExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.inner.ClassMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureFactory;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByJDK;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStream;
import java.util.List;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;

/**
 * 过滤器实现的匹配器
 *
 * @author luanjia@taobao.com
 */
public class ExtFilterMatcher implements Matcher, ClassMatcher, BehaviorMatcher {

    private final ClassMatcher classMatcher;

    private final List<BehaviorMatcher> behaviorMatchers;

    public ExtFilterMatcher(final ExtFilter extFilter) {
        this.classMatcher = new ClassExtFilterMatcher(extFilter);
        this.behaviorMatchers = Lists.newArrayList((BehaviorMatcher) new BehaviorExtFilterMatcher(extFilter));
    }

    public ExtFilterMatcher(final ClassMatcher classMatcher, List<BehaviorMatcher> behaviorMatchers) {
        this.classMatcher = classMatcher;
        this.behaviorMatchers = behaviorMatchers;
    }

    @Override
    public MatchingResult matching(final ClassStructure classStructure) {

        try {
            return _matching(classStructure);
        } catch (NoClassDefFoundError error) {

            // 根据 #203 ClassStructureImplByJDK会存在类加载异步的问题
            // 所以这里对JDK实现的ClassStructure抛出NoClassDefFoundError的时候做一个兼容
            // 转换为ASM实现然后进行match
            if (classStructure instanceof ClassStructureImplByJDK
                    && classStructure.getClassLoader() != null) {
                final String javaClassResourceName = toInternalClassName(classStructure.getJavaClassName()).concat(".class");
                InputStream is = null;
                try {
                    is = classStructure.getClassLoader().getResourceAsStream(javaClassResourceName);
                    _matching(ClassStructureFactory.createClassStructure(is, classStructure.getClassLoader()));
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

            // 其他情况就直接抛出error
            throw error;
        }

    }

    @Override
    public boolean matches(BehaviorStructure behaviorStructure) {
        for (BehaviorMatcher behaviorMatcher : behaviorMatchers) {
            if (behaviorMatcher.matches(behaviorStructure)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matches(ClassStructure classStructure) {
        return classMatcher.matches(classStructure);
    }


    private MatchingResult _matching(final ClassStructure classStructure) {
        final MatchingResult result = new MatchingResult();

        // 匹配ClassStructure
        if (!classMatcher.matches(classStructure)) {
            return result;
        }

        // 匹配BehaviorStructure
        for (final BehaviorStructure behaviorStructure : classStructure.getBehaviorStructures()) {
            if (matches(behaviorStructure)) {
                result.getBehaviorStructures().add(behaviorStructure);
            }
        }

        return result;
    }


    /**
     * 兼容{@code sandbox-api:1.0.10}时
     * 在{@link EventWatchCondition#getOrFilterArray()}中将{@link Filter}直接暴露出来的问题，
     * 所以这里做一个兼容性的强制转换
     *
     * <ul>
     * <li>如果filterArray[index]是一个{@link ExtFilter}，则不需要再次转换</li>
     * <li>如果filterArray[index]是一个{@link Filter}，则需要进行{@link ExtFilterFactory#make(Filter)}的转换</li>
     * </ul>
     *
     * @param filterArray 过滤器数组
     * @return 兼容的Matcher
     */
    public static GroupMatcher toOrGroupMatcher(final Filter[] filterArray) {
        final ExtFilter[] extFilterArray = new ExtFilter[filterArray.length];
        for (int index = 0; index < filterArray.length; index++) {
            extFilterArray[index] = ExtFilterFactory.make(filterArray[index]);
        }
        return toOrGroupMatcher(extFilterArray);
    }

    /**
     * 将{@link ExtFilter}数组转换为Or关系的Matcher
     *
     * @param extFilterArray 增强过滤器数组
     * @return Or关系Matcher
     */
    public static GroupMatcher toOrGroupMatcher(final ExtFilter[] extFilterArray) {
        final Matcher[] matcherArray = new Matcher[ArrayUtils.getLength(extFilterArray)];
        for (int index = 0; index < matcherArray.length; index++) {
            matcherArray[index] = new ExtFilterMatcher(extFilterArray[index]);
        }
        return new GroupMatcher.Or(matcherArray);
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public List<BehaviorMatcher> getBehaviorMatchers() {
        return behaviorMatchers;
    }

    @Override
    public boolean hasClassIdentity() {
        return classMatcher.hasClassIdentity();
    }

    @Override
    public String getClassIdentity() {
        return classMatcher.getClassIdentity();
    }
}
