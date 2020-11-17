package com.alibaba.jvm.sandbox.module.junit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2019-05-13 18:26
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PrepareForTest {
    Class testModule();

    Class[] spyClasses() default {};

    String[] spyClassNames() default {};

    String namespace() default "default";

    boolean isEnableUnsafe() default false;

    /**
     * Enable fast mode that only stubs when {@link #spyClasses} and {@link #spyClassNames} identified.
     * Notice that disabling the fast mode may come with a significant performance overhead.
     *
     * @return
     */
    boolean fastMode() default true;

}
