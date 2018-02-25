package com.alibaba.jvm.sandbox.api.listener.ext;

import com.alibaba.jvm.sandbox.api.filter.Filter;

/**
 * 事件观察条件
 *
 * @author luanjia@taobao.com
 * @since {@code sandbox-api:1.0.10}
 */
public interface EventWatchCondition {

    /**
     * 获取"或"关系的查询过滤器数组
     *
     * @return "或"关系的查询过滤器数组
     */
    Filter[] getOrFilterArray();

}
