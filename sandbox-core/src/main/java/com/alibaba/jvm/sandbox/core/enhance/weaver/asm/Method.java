package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

/**
 * @author zhuangpeng
 * @since 2020/9/13
 */
public class Method extends org.objectweb.asm.commons.Method {

    public int access;

    public Method(int access ,String name, String descriptor) {
        super(name, descriptor);
        this.access = access;
    }
}
