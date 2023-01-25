package com.alibaba.jvm.sandbox.module.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.jvm.sandbox.api.util.GaArrayUtils.isEmpty;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 命令参数支撑类
 */
public class ParamSupported {

    /**
     * 转换器(字符串到指定类型的转换器)
     *
     * @param <T> 转换目标类型
     */
    interface Converter<T> {

        /**
         * 转换字符串为目标类型
         *
         * @param string 字符串内容
         * @return 目标类型
         */
        T convert(String string);
    }

    // 转换器集合
    final static Map<Class<?>, Converter<?>> converterMap = new HashMap<>();

    static {

        // 转换为字符串
        //noinspection unchecked
        regConverter(string -> string, String.class);

        // 转换为Long
        //noinspection unchecked
        regConverter(Long::valueOf, long.class, Long.class);

        // 转换为Double
        //noinspection unchecked
        regConverter(Double::valueOf, double.class, Double.class);

        // 转换为Integer
        //noinspection unchecked
        regConverter(Integer::valueOf, int.class, Integer.class);

    }

    /**
     * 注册类型转换器
     *
     * @param converter 转换器
     * @param typeArray 类型的Java类数组
     * @param <T>       类型
     */
    @SuppressWarnings("unchecked")
    protected static <T> void regConverter(Converter<T> converter, Class<T>... typeArray) {
        for (final Class<T> type : typeArray) {
            converterMap.put(type, converter);
        }
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Converter<T> converter,
                                        final T defaultValue) {
        final String string = param.get(name);
        return isNotBlank(string)
                ? converter.convert(string)
                : defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected static <T> List<T> getParameters(final Map<String, String[]> param,
                                               final String name,
                                               final Converter<T> converter,
                                               final T... defaultValueArray) {
        final String[] stringArray = param.get(name);
        if (isEmpty(stringArray)) {
            return asList(defaultValueArray);
        }
        final List<T> values = new ArrayList<>();
        for (final String string : stringArray) {
            values.add(converter.convert(string));
        }
        return values;
    }


    protected static String getParameter(final Map<String, String> param,
                                         final String name) {
        return getParameter(
                param,
                name,
                String.class,
                null
        );
    }

    protected static String getParameter(final Map<String, String> param,
                                         final String name,
                                         final String defaultString) {
        return getParameter(
                param,
                name,
                String.class,
                defaultString
        );
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Class<T> type) {
        return getParameter(
                param,
                name,
                type,
                null
        );
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Class<T> type,
                                        final T defaultValue) {
        //noinspection unchecked
        return getParameter(
                param,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValue
        );
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    protected static <T> List<T> getParameters(final Map<String, String[]> param,
                                               final String name,
                                               final Class<T> type,
                                               final T... defaultValueArray) {
        //noinspection unchecked
        return getParameters(
                param,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValueArray
        );
    }


}
