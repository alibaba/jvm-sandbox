package com.alibaba.jvm.sandbox.core.util.matcher.inner;

import com.alibaba.jvm.sandbox.api.filter.ExtFilter;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;

import static com.alibaba.jvm.sandbox.core.util.ClassStructureUtils.toJavaClassNameArray;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/9 1:13 上午
 */
public class BehaviorExtFilterMatcher implements BehaviorMatcher {

    private final ExtFilter extFilter;

    public BehaviorExtFilterMatcher(ExtFilter extFilter) {
        this.extFilter = extFilter;
    }

    @Override
    public boolean matches(BehaviorStructure behaviorStructure) {
        return extFilter.doMethodFilter(
                behaviorStructure.getAccess().getAccessCode(),
                behaviorStructure.getName(),
                toJavaClassNameArray(behaviorStructure.getParameterTypeClassStructures()),
                toJavaClassNameArray(behaviorStructure.getExceptionTypeClassStructures()),
                toJavaClassNameArray(behaviorStructure.getAnnotationTypeClassStructures()));
    }
}
