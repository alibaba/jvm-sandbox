package com.alibaba.jvm.sandbox.qatest.api;

import com.alibaba.jvm.sandbox.api.listener.ext.Behavior;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BehaviorTestCase {

    @Test
    public void test$behavior$method() throws NoSuchMethodException {
        final Method method = String.class.getMethod("toString");
        final Behavior behavior = new Behavior.MethodImpl(method);
        Assert.assertEquals(method, behavior.getTarget());
        Assert.assertEquals(method.getName(), behavior.getName());
        Assert.assertEquals(method.isAccessible(), behavior.isAccessible());
        {
            final boolean access = behavior.isAccessible();
            try {
                behavior.setAccessible(!access);
                Assert.assertEquals(!access, behavior.isAccessible());
                Assert.assertEquals(!access, method.isAccessible());
            } finally {
                behavior.setAccessible(access);
                Assert.assertEquals(access, behavior.isAccessible());
                Assert.assertEquals(access, method.isAccessible());
            }
        }
        Assert.assertEquals(method.getModifiers(), behavior.getModifiers());
        Assert.assertEquals(method.getDeclaringClass(), behavior.getDeclaringClass());
        Assert.assertEquals(method.getReturnType(), behavior.getReturnType());
        Assert.assertArrayEquals(method.getParameterTypes(), behavior.getParameterTypes());
        Assert.assertArrayEquals(method.getExceptionTypes(), behavior.getExceptionTypes());
        Assert.assertArrayEquals(method.getAnnotations(), behavior.getAnnotations());
        Assert.assertArrayEquals(method.getDeclaredAnnotations(), behavior.getDeclaredAnnotations());
    }

    @Test
    public void test$behavior$constructor() throws NoSuchMethodException {
        final Constructor<?> constructor = String.class.getConstructor(String.class);
        final Behavior behavior = new Behavior.ConstructorImpl(constructor);
        Assert.assertEquals(constructor, behavior.getTarget());
        Assert.assertEquals("<init>", behavior.getName());
        Assert.assertEquals(constructor.isAccessible(), behavior.isAccessible());
        {
            final boolean access = behavior.isAccessible();
            try {
                behavior.setAccessible(!access);
                Assert.assertEquals(!access, behavior.isAccessible());
                Assert.assertEquals(!access, constructor.isAccessible());
            } finally {
                behavior.setAccessible(access);
                Assert.assertEquals(access, behavior.isAccessible());
                Assert.assertEquals(access, constructor.isAccessible());
            }
        }
        Assert.assertEquals(constructor.getModifiers(), behavior.getModifiers());
        Assert.assertEquals(constructor.getDeclaringClass(), behavior.getDeclaringClass());
        Assert.assertEquals(constructor.getDeclaringClass(), behavior.getReturnType());
        Assert.assertArrayEquals(constructor.getParameterTypes(), behavior.getParameterTypes());
        Assert.assertArrayEquals(constructor.getExceptionTypes(), behavior.getExceptionTypes());
        Assert.assertArrayEquals(constructor.getAnnotations(), behavior.getAnnotations());
        Assert.assertArrayEquals(constructor.getDeclaredAnnotations(), behavior.getDeclaredAnnotations());
    }

}
