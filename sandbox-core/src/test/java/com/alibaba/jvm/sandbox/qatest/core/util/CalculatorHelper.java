package com.alibaba.jvm.sandbox.qatest.core.util;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.filter.NameRegexFilter;
import com.alibaba.jvm.sandbox.core.util.UnCaughtException;
import com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator;

import java.lang.reflect.InvocationTargetException;

import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.getJavaClassName;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtInvokeMethod;

/**
 * 计算器辅助类
 */
public class CalculatorHelper {

    public static final String CALCULATOR_CLASS_NAME = getJavaClassName(Calculator.class);

    /**
     * 拦截sum()方法过滤器
     */
    public static final Filter CALCULATOR_SUM_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^sum$"
    );

    /**
     * 拦截sum()和add()方法过滤器
     */
    public static final Filter CALCULATOR_SUM_and_ADD_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^(sum|add)$"
    );

    /**
     * 拦截errorSum()方法过滤器
     */
    public static final Filter CALCULATOR_ERROR_SUM_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^errorSum$"
    );

    /**
     * 调用sum()方法
     *
     * @param calculatorObject 目标计算器对象实例
     * @param numArray         参数
     * @return 返回值
     * @throws Throwable 调用失败
     */
    public static int sum(final Object calculatorObject, int... numArray) throws Throwable {
        try {
            return unCaughtInvokeMethod(
                    unCaughtGetClassDeclaredJavaMethod(calculatorObject.getClass(), "sum", int[].class),
                    calculatorObject,
                    numArray
            );
        } catch (Throwable cause) {
            if (cause instanceof UnCaughtException
                    && (cause.getCause() instanceof InvocationTargetException)) {
                throw ((InvocationTargetException) cause.getCause()).getTargetException();
            }
            throw cause;
        }

    }

    /**
     * 调用sum()方法
     *
     * @param calculatorObject pow();
     * @param num              num
     * @param n                n次方
     * @return 返回值
     * @throws Throwable 调用失败
     */
    public static int pow(final Object calculatorObject, int num, int n) throws Throwable {
        try {
            return unCaughtInvokeMethod(
                    unCaughtGetClassDeclaredJavaMethod(calculatorObject.getClass(), "pow", int.class, int.class),
                    calculatorObject,
                    num,
                    n
            );
        } catch (Throwable cause) {
            if (cause instanceof UnCaughtException
                    && (cause.getCause() instanceof InvocationTargetException)) {
                throw ((InvocationTargetException) cause.getCause()).getTargetException();
            }
            throw cause;
        }
    }

    public static Object newInstance(final Class<?> calculatorClass) throws Throwable {
        try {
            return calculatorClass.getConstructor().newInstance();
        } catch (InvocationTargetException cause) {
            throw cause.getTargetException();
        }
    }

    public static Object newInstance(final Class<?> calculatorClass, final Calculator.TestCase tCase) throws Throwable {
        try {
            return calculatorClass.getConstructor(String.class).newInstance(tCase.name());
        } catch (InvocationTargetException cause) {
            throw cause.getTargetException();
        }
    }

}
