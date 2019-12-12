package com.alibaba.jvm.sandbox.qatest.core.enhance.target;

import static com.alibaba.jvm.sandbox.qatest.core.enhance.target.Calculator.TestCase.*;

/**
 * 计算器类（靶机类：所有方法拦截都基于这个类进行）
 */
@InheritedComputer
@Computer
public class Calculator {

    public static final String ERROR_EXCEPTION_MESSAGE = "THIS IS A TEST CAME FROM CALCULATOR!";

    /**
     * 计算器异常
     */
    public static class CalculatorException extends RuntimeException {
        public CalculatorException(String message) {
            super(message);
        }
    }

    /**
     * 用例场景
     */
    public enum TestCase {

        /**
         * 构造函数中抛出异常
         */
        INIT_WITH_TEST_CASE$EXCEPTION,

        /**
         * sum()中抛出异常
         */
        SUM$EXCEPTION,

        /**
         * add()中抛出异常
         */
        ADD$EXCEPTION,

        /**
         * pow()递归中最后一层递归抛出异常
         */
        POW$EXCEPTION$AT_LAST,

        /**
         * 正常返回
         */
        NONE

    }

    private final TestCase tCase;

    public Calculator() {
        this(NONE);
    }

    public Calculator(String tCaseName) {
        this(TestCase.valueOf(tCaseName));
    }

    public Calculator(TestCase tCase) {
        this.tCase = tCase;
        if (tCase == INIT_WITH_TEST_CASE$EXCEPTION) {
            throwCalculatorException();
        }
    }

    /**
     * 求两数之和
     *
     * @param a a
     * @param b b
     * @return a+b
     */
    public int add(int a, int b) {
        if (tCase == ADD$EXCEPTION) {
            throwCalculatorException();
        }
        return a + b;
    }

    /**
     * 求一个数组之和（嵌套方法）
     *
     * @param numArray 数组
     * @return 数组之和
     */
    public int sum(int... numArray) {
        if (tCase == SUM$EXCEPTION) {
            throwCalculatorException();
        }
        int r = 0;
        for (int n : numArray) {
            r = add(r, n);
        }
        return r;
    }

    /**
     * 求num的n次方（递归方法）
     *
     * @param num num
     * @param n   n次方
     * @return num的n次方
     */
    public int pow(int num, int n) {
        if (n == 0) {
            if (tCase == POW$EXCEPTION$AT_LAST) {
                throwCalculatorException();
            }
            return 1;
        }
        return num * pow(num, n - 1);
    }

    /**
     * 模拟抛出异常的方法
     */
    private static void throwCalculatorException() {
        throw new RuntimeException(ERROR_EXCEPTION_MESSAGE);
    }

    private static TestCase tCaseInStatic;

    /**
     * 静态方法-求两数之和
     * @param a 数字1
     * @param b 数字2
     * @return a+b
     */
    public static int addInStatic(int a,int b){
        if(tCaseInStatic == ADD$EXCEPTION){
            throwCalculatorException();
        }
        return a+b;
    }

    /**
     * 通过静态方法设置异常变量信息
     * @param tCase 异常信息
     */
    public static void settCaseInStatic(TestCase tCase){
        tCaseInStatic=tCase;
    }

}
