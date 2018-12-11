package com.alibaba.jvm.sandbox.qatest.core.enhance.target;

public class Calculator {

    public static int sum(int... numArray) {
        int r = 0;
        for (int n : numArray) {
            r = add(r, n);
        }
        return r;
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int errorSum(int... numArray) {
        throwsRuntimeException("THIS IS A TEST!");
        return 0;
    }

    private static void throwsRuntimeException(String message) {
        throw new RuntimeException(message);
    }

}
