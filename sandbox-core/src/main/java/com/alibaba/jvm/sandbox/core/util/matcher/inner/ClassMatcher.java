package com.alibaba.jvm.sandbox.core.util.matcher.inner;

import com.alibaba.jvm.sandbox.api.filter.ClassIdentifiable;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructure;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/8 11:11 下午
 */
public interface ClassMatcher extends ClassIdentifiable {

    /**
     * 匹配类结构
     *
     * @param classStructure 类结构
     * @return 匹配结果
     */
    boolean matches(ClassStructure classStructure);

}
