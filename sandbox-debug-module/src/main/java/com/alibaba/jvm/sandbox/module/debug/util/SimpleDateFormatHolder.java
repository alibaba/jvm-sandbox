package com.alibaba.jvm.sandbox.module.debug.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SimpleDateFormat Holder
 *
 * @author oldmanpushcart@gmail.com
 */
public class SimpleDateFormatHolder extends ThreadLocal<SimpleDateFormat> {

    @Override
    protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private static final SimpleDateFormatHolder instance = new SimpleDateFormatHolder();

    private SimpleDateFormatHolder() {
        //
    }

    public static SimpleDateFormatHolder getInstance() {
        return instance;
    }

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 格式化后字符串
     */
    public String format(Date date) {
        return getInstance().get().format(date);
    }


    /**
     * 格式化日期
     *
     * @param gmt gmt
     * @return 格式化后字符串
     */
    public String format(long gmt) {
        return getInstance().get().format(new Date(gmt));
    }

}
