package com.alibaba.jvm.sandbox.core.util.matcher.inner;

import com.alibaba.jvm.sandbox.core.util.matcher.structure.BehaviorStructure;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/8 11:11 下午
 */
public interface BehaviorMatcher {

    /**
     * 匹配函数结构
     *
     * @param behaviorStructure 函数结构
     * @return 匹配结果
     */
    boolean matches(BehaviorStructure behaviorStructure);

}
