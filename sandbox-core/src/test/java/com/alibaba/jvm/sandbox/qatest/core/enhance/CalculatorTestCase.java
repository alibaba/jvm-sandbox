package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.filter.NameRegexFilter;
import com.alibaba.jvm.sandbox.core.util.UnCaughtException;

import java.lang.reflect.InvocationTargetException;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtInvokeMethod;

public class CalculatorTestCase extends CoreEnhanceBaseTestCase {

    public static final Filter CALCULATOR_SUM_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^sum$"
    );

    public static final Filter CALCULATOR_SUM_and_ADD_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^(sum|add)$"
    );

    public static final Filter CALCULATOR_ERROR_SUM_FILTER
            = new NameRegexFilter(
            "^com\\.alibaba\\.jvm.sandbox\\.qatest\\.core\\.enhance\\.target\\.Calculator$",
            "^errorSum$"
    );

    protected int calculatorSum(final Object calculatorObject, int... numArray) throws Throwable {
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

    protected int calculatorErrorSum(final Object calculatorObject, int... numArray) throws Throwable {
        try {
            return unCaughtInvokeMethod(
                    unCaughtGetClassDeclaredJavaMethod(calculatorObject.getClass(), "errorSum", int[].class),
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

}
