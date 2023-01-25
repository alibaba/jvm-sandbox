package com.alibaba.jvm.sandbox.core.util;

import com.alibaba.jvm.sandbox.api.util.GaArrayUtils;
import com.alibaba.jvm.sandbox.api.util.GaStringUtils;

/**
 * 内核用字符串工具类
 */
public class CoreStringUtils {

    /**
     * 判断目标字符串是否包含任一搜索字符串
     * @param string 目标字符串
     * @param searchStrings 搜索字符串集合
     * @return TRUE | FALSE
     */
    public static boolean containsAny(final String string, final String... searchStrings) {
        if(GaStringUtils.isEmpty(string) || GaArrayUtils.isEmpty(searchStrings)) {
            return false;
        }
        for(final String searchString: searchStrings) {
            if(string.contains(searchString)) {
                return true;
            }
        }
        return false;
    }

}
