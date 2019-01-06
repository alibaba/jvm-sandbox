package com.alibaba.jvm.sandbox.qatest.core.mock;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.LoadedClassLoaderListener;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EmptyCoreLoadedClassDataSource implements CoreLoadedClassDataSource {
    @Override
    public List<Class<?>> findForReTransform(Matcher matcher) {
        return null;
    }

    @Override
    public ClassLoader[] listLoadedClassLoader() {
        return new ClassLoader[0];
    }

    @Override
    public void appendLoadedClassLoaderListener(LoadedClassLoaderListener listener) {

    }

    @Override
    public void removeLoadedClassLoaderListener(LoadedClassLoaderListener listener) {

    }

    @Override
    public Set<Class<?>> list() {
        return null;
    }

    @Override
    public Set<Class<?>> find(Filter filter) {
        return null;
    }

    @Override
    public Iterator<Class<?>> iteratorForLoadedClasses() {
        return null;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return new byte[0];
    }
}
