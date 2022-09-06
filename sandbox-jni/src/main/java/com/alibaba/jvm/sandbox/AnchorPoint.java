package com.alibaba.jvm.sandbox;

/**
 * {@link AnchorPoint}
 * <p>
 *
 * @author zhaoyb1990
 */
public interface AnchorPoint {

    /**
     * 获取klass在当前jvm中的实例对象
     *
     * @param klass 目标类型
     * @param limit 返回条数
     * @param <T>   泛型
     * @return 实例列表
     */
    <T> T[] getInstances(Class<T> klass, int limit);

    /**
     * 获取klass在当前jvm中的实例对象
     *
     * @param klass 目标类型
     * @param <T>   泛型
     * @return 实例列表
     */
    <T> T[] getInstances(Class<T> klass);
}
