package com.alibaba.jvm.sandbox.qatest.core.enhance.target;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface InheritedComputer {
}
