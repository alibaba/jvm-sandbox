package com.alibaba.jvm.sandbox.core.enhance;

/**
 * 代码增强
 * Created by luanjia@taobao.com on 16/7/18.
 */
public interface Enhancer {

    /**
     * 转换为增强后的字节码数组
     *
     * @param loader           目标类加载器
     * @param srcByteCodeArray 源字节码数组
     * @return 增强后的字节码数组
     */
    byte[] toByteCodeArray(ClassLoader loader, byte[] srcByteCodeArray);

}
