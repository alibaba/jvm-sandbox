package com.alibaba.jvm.sandbox.api.resource;

import com.alibaba.jvm.sandbox.api.filter.Filter;

import java.util.Set;

/**
 * 已加载类数据源
 *
 * @author luanjia@taobao.com
 */
public interface LoadedClassDataSource {

    /**
     * 获取所有已加载的类集合
     *
     * @return 所有已加载的类集合
     */
    Set<Class<?>> list();

    /**
     * 根据过滤器搜索出匹配的类集合
     *
     * @param filter 扩展过滤器
     * @return 匹配的类集合
     */
    Set<Class<?>> find(Filter filter);

}
