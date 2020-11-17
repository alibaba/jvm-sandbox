package com.alibaba.jvm.sandbox.module.junit.mock;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Less classes for faster testing.
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2019-05-13 21:06
 */
public class MockLoadedClassesOnlyInstrumentation implements Instrumentation {

    private final Set<Class<?>> loadedClasses = new LinkedHashSet<>();

    private static Instrumentation instrumentation = ByteBuddyAgent.install();

    public void regLoadedClass(Class<?> clazz) {
        loadedClasses.add(clazz);
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return loadedClasses.toArray(new Class<?>[]{});
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return instrumentation.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return instrumentation.getObjectSize(objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        instrumentation.appendToSystemClassLoaderSearch(jarfile);
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return instrumentation.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        instrumentation.setNativeMethodPrefix(transformer, prefix);
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return instrumentation.isModifiableClass(theClass);
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        instrumentation.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return instrumentation.isRedefineClassesSupported();
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
        instrumentation.redefineClasses(definitions);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return instrumentation.isRetransformClassesSupported();
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        instrumentation.addTransformer(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        return instrumentation.removeTransformer(transformer);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        instrumentation.addTransformer(transformer, canRetransform);
    }
}
