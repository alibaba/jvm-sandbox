package com.alibaba.jvm.sandbox.core.enhance.weaver;

/**
 * @author zhuangpeng
 * @since 2020/1/2
 */
public class ImmediatelyThrowException extends Throwable{
    // 需要被抛出的目标异常
    private Throwable target;

    public ImmediatelyThrowException(Throwable target){
        this.target = target;
    }

    public Throwable getTarget(){
        return this.target;
    }
}
