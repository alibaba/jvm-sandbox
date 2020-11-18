package com.alibaba.jvm.sandbox.core.util;

import com.alibaba.jvm.sandbox.api.filter.AccessFlags;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.Access;

import static com.alibaba.jvm.sandbox.api.filter.AccessFlags.*;
import static com.alibaba.jvm.sandbox.api.filter.AccessFlags.ACF_ANNOTATION;

/**
 * Utilize for {@link Access}
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/9 1:17 上午
 */
public abstract class AccessUtils {

    /**
     * 转换为{@link AccessFlags}的Access体系
     *
     * @param access access flag
     * @return 部分兼容ASM的access flag
     */
    public static int toFilterAccess(final Access access) {
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
