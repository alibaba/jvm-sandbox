package com.alibaba.jvm.sandbox.module.junit.runner;

import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.annotations.SandboxTestConfig;
import com.alibaba.jvm.sandbox.module.junit.support.ReflectionUtils;
import com.google.common.collect.Sets;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A runner that manage the class loading
 *
 * @deprecated this class is used for the early version
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2019-05-13 19:35
 */
@Deprecated
public class SandboxRunner extends BlockJUnit4ClassRunner {

    private static final Pattern SANDBOX_CORE_PATTERN = Pattern.compile("com\\.alibaba\\.jvm\\.sandbox\\.core\\..*");

    private static final Pattern SANDBOX_JUNIT_PATTERN = Pattern.compile("com\\.alibaba\\.jvm\\.sandbox\\.module\\.junit\\..*");

    public SandboxRunner(Class<?> klass) throws InitializationError {
        super(getFromTestClassloader(klass));
    }

    private static void fulfill(TestClassLoader testClassLoader, Class<?> clazz) {

        SandboxTestConfig config = clazz.getAnnotation(SandboxTestConfig.class);

        if (config != null) {
            if (config.spySandboxCore()) {
                testClassLoader.addSpyClassNamePattern(SANDBOX_CORE_PATTERN);
            }
            if (config.spySandboxJunit()) {
                testClassLoader.addSpyClassNamePattern(SANDBOX_JUNIT_PATTERN);
            }
            String[] patterns = config.spyPatterns();
            if (patterns.length > 0) {
                for (String pattern : patterns) {
                    testClassLoader.addSpyClassNamePattern(Pattern.compile(pattern));
                }
            }
        }

        // First, get the base list of tests
        final List<Method> allMethods = ReflectionUtils.getMethodsAnnotatedWith(clazz, PrepareForTest.class);
        if (allMethods.size() == 0) {
            return;
        }

        for (Method method : allMethods) {
            PrepareForTest prepareForTest = method.getAnnotation(PrepareForTest.class);
            testClassLoader.addSpyClasses(prepareForTest.spyClasses());
            String[] names = prepareForTest.spyClassNames();
            for (String name : names) {
                testClassLoader.addSpyClassNamePattern(Pattern.compile(name));
            }
        }

    }

    private static Class<?> getFromTestClassloader(Class<?> clazz) throws InitializationError {
        try {
            TestClassLoader testClassLoader = new TestClassLoader(clazz);
            fulfill(testClassLoader, clazz);
            return Class.forName(clazz.getName(), true, testClassLoader);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    public static class TestClassLoader extends URLClassLoader {

        private Set<String> spyClasses = Sets.newConcurrentHashSet();

        private Set<Pattern> spyClassNamePatterns = Sets.newConcurrentHashSet();

        private Set<String> loadedClass = Sets.newConcurrentHashSet();

        TestClassLoader(Class<?> clazz) {
            super(((URLClassLoader) getSystemClassLoader()).getURLs());
            spyClasses.add(clazz.getName());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {

            if (name.startsWith("java.")) {
                loadedClass.add(name);
            }

            // the class should be spied if matches
            for (Pattern spyClassNamePattern : spyClassNamePatterns) {
                if (spyClassNamePattern.matcher(name).matches()) {
                    spyClasses.add(name);
                }
            }

            if (spyClasses.contains(name) && !loadedClass.contains(name)) {
                loadedClass.add(name);
                System.out.println("reload:" + name);
                return super.findClass(name);
            }

            return super.loadClass(name);

        }

        void addSpyClasses(Class<?>[] classes) {
            for (Class<?> aClass : classes) {
                spyClasses.add(aClass.getName());
            }
        }

        void addSpyClassNamePattern(Pattern pattern) {
            spyClassNamePatterns.add(pattern);
        }

    }

}
