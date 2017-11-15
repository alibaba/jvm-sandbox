package com.alibaba.jvm.sandbox.core.enhance.annotation;

import java.lang.annotation.*;

/**
 * 中断式事件处理器
 * 当事件处理器处理事件抛出异常时,将会中断原有方法调用
 * Created by luanjia@taobao.com on 16/7/26.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Interrupted {
}
