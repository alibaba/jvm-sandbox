package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.util.GaArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class HttpSupported {

    /**
     * 封装HTTP错误码
     */
    protected static class HttpErrorCodeException extends Exception {
        private final int code;

        HttpErrorCodeException(int code, String message) {
            super(message);
            this.code = code;
        }

        int getCode() {
            return code;
        }

    }

    interface Converter<T> {
        T convert(String string);
    }

    static Map<Class<?>, Converter<?>> converterMap = new HashMap<Class<?>, Converter<?>>();

    static {
        regConverter(new Converter<String>() {
            @Override
            public String convert(String string) {
                return string;
            }
        }, String.class);

        regConverter(new Converter<Long>() {
            @Override
            public Long convert(String string) {
                return Long.valueOf(string);
            }
        }, long.class, Long.class);

        regConverter(new Converter<Double>() {
            @Override
            public Double convert(String string) {
                return Double.valueOf(string);
            }
        }, double.class, Double.class);

        regConverter(new Converter<Integer>() {
            @Override
            public Integer convert(String string) {
                return Integer.valueOf(string);
            }
        }, int.class, Integer.class);

    }

    /**
     * 注册类型转换器
     *
     * @param converter 转换器
     * @param typeArray 类型的Java类数组
     * @param <T>       类型
     */
    protected static <T> void regConverter(Converter<T> converter, Class<T>... typeArray) {
        for (final Class<T> type : typeArray) {
            converterMap.put(type, converter);
        }
    }

    private static <T> T convert(final Converter<T> converter,
                                 final String string,
                                 final String name) throws HttpErrorCodeException {
        if (null == converter) {
            throw new HttpErrorCodeException(SC_BAD_REQUEST, String.format("parameter:%s is unknow type!", name));
        }
        try {
            return converter.convert(string);
        } catch (Throwable cause) {
            throw new HttpErrorCodeException(SC_BAD_REQUEST, String.format("paramater:%s is illegal format!", name));
        }
    }

    protected static <T> T getParameter(final HttpServletRequest req,
                                        final String name,
                                        final Converter<T> converter,
                                        final T defaultValue) throws HttpErrorCodeException {
        final String string = req.getParameter(name);

        // 参数值为空
        if (StringUtils.isBlank(string)) {
            if (null == defaultValue) {
                throw new HttpErrorCodeException(SC_BAD_REQUEST, String.format("parameter:%s is require!", name));
            }
            return defaultValue;
        }

        // 参数值不为空
        else {
            return convert(converter, string, name);
        }
    }

    protected static <T> List<T> getParameters(final HttpServletRequest req,
                                               final String name,
                                               final Converter<T> converter,
                                               final T... defaultValueArray) throws HttpErrorCodeException {
        final String[] stringArray = req.getParameterValues(name);
        if (GaArrayUtils.isEmpty(stringArray)) {
            if (null == defaultValueArray) {
                throw new HttpErrorCodeException(SC_BAD_REQUEST, String.format("parameter:%s is require!", name));
            }
            return Arrays.asList(defaultValueArray);
        }
        final List<T> values = new ArrayList<T>();
        for (final String string : stringArray) {
            values.add(convert(converter, string, name));
        }
        return values;
    }


    protected static String getParameter(final HttpServletRequest req,
                                         final String name) throws HttpErrorCodeException {
        return getParameter(
                req, name,
                String.class,
                null
        );
    }

    protected static String getParameter(final HttpServletRequest req,
                                         final String name,
                                         final String defaultString) throws HttpErrorCodeException {
        return getParameter(
                req,
                name,
                String.class,
                defaultString
        );
    }

    protected static <T> T getParameter(final HttpServletRequest req,
                                        final String name,
                                        final Class<T> type) throws HttpErrorCodeException {
        return getParameter(
                req,
                name,
                type,
                null
        );
    }

    protected static <T> T getParameter(final HttpServletRequest req,
                                        final String name,
                                        final Class<T> type,
                                        final T defaultValue) throws HttpErrorCodeException {
        return getParameter(
                req,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValue
        );
    }

    protected static <T> List<T> getParameters(final HttpServletRequest req,
                                               final String name,
                                               final Class<T> type,
                                               final T... defaultValueArray) throws HttpErrorCodeException {
        return getParameters(
                req,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValueArray
        );
    }


}
