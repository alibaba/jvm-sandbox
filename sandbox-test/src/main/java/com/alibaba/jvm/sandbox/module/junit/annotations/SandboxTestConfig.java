package com.alibaba.jvm.sandbox.module.junit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated this class is used for the early version
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2019-06-16 18:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface SandboxTestConfig {

    boolean spySandboxCore() default false;

    boolean spySandboxJunit() default false;

    String[] spyPatterns() default {};

}
