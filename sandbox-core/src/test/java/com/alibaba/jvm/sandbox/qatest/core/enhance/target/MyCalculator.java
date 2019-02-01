package com.alibaba.jvm.sandbox.qatest.core.enhance.target;

public class MyCalculator extends Calculator {

    public MyCalculator(String tCaseName) {
        super(tCaseName
                .toUpperCase());
    }

    public MyCalculator() {
        super();
    }

    @Override
    public int sum(int... numArray) {
        return super.sum(numArray);
    }

}
