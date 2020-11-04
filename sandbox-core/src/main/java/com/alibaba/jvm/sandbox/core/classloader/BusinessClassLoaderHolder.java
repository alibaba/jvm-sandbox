package com.alibaba.jvm.sandbox.core.classloader;

/**
 * @author zhuangpeng
 * @since 2020/1/15
 */
public class BusinessClassLoaderHolder {

    private static final ThreadLocal<DelegateBizClassLoader> holder = new ThreadLocal<DelegateBizClassLoader>();

    public static void setBussinessClassLoader(ClassLoader classLoader){
        if(null == classLoader){
            return;
        }
        DelegateBizClassLoader delegateBizClassLoader = new DelegateBizClassLoader(classLoader);
        holder.set(delegateBizClassLoader);
    }


    public static void removeBussinessClassLoader(){
        holder.remove();
    }

    public static DelegateBizClassLoader getBussinessClassLoader(){

        return holder.get();
    }

    public static class DelegateBizClassLoader extends ClassLoader{
        public DelegateBizClassLoader(ClassLoader parent){
            super(parent);
        }

        @Override
        public Class<?> loadClass(final String javaClassName, final boolean resolve) throws ClassNotFoundException {
            return super.loadClass(javaClassName,resolve);
        }
    }
}
