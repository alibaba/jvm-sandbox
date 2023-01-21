package com.alibaba.jvm.sandbox.api.listener.ext;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

/**
 * 类行为，主要用来封装构造函数cinit/init/method
 * 具体可以参考{@link Method}和{@link Constructor}
 *
 * @author oldmanpushcart@gmail.com
 * @since {@code sandbox-api:1.0.10}
 */
public interface Behavior {

    Object invoke(Object obj, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException;

    boolean isAccessible();

    void setAccessible(boolean accessFlag);

    String getName();

    Class<?>[] getParameterTypes();

    Annotation[] getAnnotations();

    int getModifiers();

    Class<?> getDeclaringClass();

    Class<?> getReturnType();

    /**
     * @see Method#getGenericReturnType()
     * @since {@code sandbox-api:1.4.0}
     */
    Type getGenericReturnType();

    Class<?>[] getExceptionTypes();

    Annotation[] getDeclaredAnnotations();

    /**
     * 获取被封装的目标对象: Method/Constructor
     *
     * @return 目标对象
     * @since {@code sandbox-api:1.2.2}
     */
    AccessibleObject getTarget();

    /**
     * 类实现
     */
    class MethodImpl implements Behavior {

        private final Method target;

        public MethodImpl(Method target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object obj, Object... args)
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return target.invoke(obj, args);
        }

        @Override
        public boolean isAccessible() {
            return target.isAccessible();
        }

        @Override
        public void setAccessible(boolean accessFlag) {
            target.setAccessible(accessFlag);
        }

        @Override
        public String getName() {
            return target.getName();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return target.getParameterTypes();
        }

        @Override
        public Annotation[] getAnnotations() {
            return target.getAnnotations();
        }

        @Override
        public int getModifiers() {
            return target.getModifiers();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return target.getDeclaringClass();
        }

        @Override
        public Class<?> getReturnType() {
            return target.getReturnType();
        }

        @Override
        public Type getGenericReturnType() {
            return target.getGenericReturnType();
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return target.getExceptionTypes();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return target.getDeclaredAnnotations();
        }

        @Override
        public AccessibleObject getTarget() {
            return target;
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Method) {
                return target.equals(obj);
            }
            return false;
        }
    }

    /**
     * 构造函数实现
     */
    class ConstructorImpl implements Behavior {

        private final Constructor<?> target;

        public ConstructorImpl(Constructor<?> target) {
            this.target = target;
        }


        @Override
        public Object invoke(Object obj, Object... args)
                throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return target.newInstance(args);
        }

        @Override
        public boolean isAccessible() {
            return target.isAccessible();
        }

        @Override
        public void setAccessible(boolean accessFlag) {
            target.setAccessible(accessFlag);
        }

        @Override
        public String getName() {
            return "<init>";
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return target.getParameterTypes();
        }

        @Override
        public Annotation[] getAnnotations() {
            return target.getAnnotations();
        }

        @Override
        public int getModifiers() {
            return target.getModifiers();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return target.getDeclaringClass();
        }

        @Override
        public Class<?> getReturnType() {
            return target.getDeclaringClass();
        }

        @Override
        public Type getGenericReturnType() {
            return target.getDeclaringClass();
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return target.getExceptionTypes();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return target.getDeclaredAnnotations();
        }

        @Override
        public AccessibleObject getTarget() {
            return target;
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Constructor<?>) {
                return target.equals(obj);
            }
            return false;
        }

    }
}