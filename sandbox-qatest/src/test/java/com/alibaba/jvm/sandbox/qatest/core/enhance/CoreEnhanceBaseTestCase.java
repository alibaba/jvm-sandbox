package com.alibaba.jvm.sandbox.qatest.core.enhance;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.ExtFilter;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.enhance.EventEnhancer;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils;
import com.alibaba.jvm.sandbox.core.util.matcher.ExtFilterMatcher;
import com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByJDK;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;

public class CoreEnhanceBaseTestCase {

    private static final AtomicInteger LISTENER_ID_SEQ = new AtomicInteger(1000);

    /**
     * 目标Class文件转换为字节码数组
     *
     * @param targetClass 目标Class文件
     * @return 目标Class文件字节码数组
     * @throws IOException 转换出错
     */
    protected byte[] toByteArray(final Class<?> targetClass) throws IOException {
        final InputStream is = targetClass.getClassLoader().getResourceAsStream(toResourceName(targetClass.getName()));
        try {
            return IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private String toResourceName(String javaClassName) {
        return toInternalClassName(javaClassName).concat(".class");
    }

    private class TestClassLoader extends ClassLoader {

        private final Map<String,byte[]> javaClassByteArrayMap
                = new HashMap<String, byte[]>();

//        @Override
//        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//            final Class<?> loadedClass = findLoadedClass(name);
//            if (loadedClass == null) {
//                try {
//                    final Class<?> aClass = findClass(name);
//                    if (resolve) {
//                        resolveClass(aClass);
//                    }
//                    return aClass;
//                } catch (Exception e) {
//                    return super.loadClass(name, resolve);
//                }
//            } else {
//                return loadedClass;
//            }
//        }

        public Class<?> defineClass(final String javaClassName,
                                final byte[] classByteArray) throws InvocationTargetException, IllegalAccessException {
            javaClassByteArrayMap.put(toResourceName(javaClassName), classByteArray);
            return SandboxReflectUtils.defineClass(this, javaClassName, classByteArray);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if(javaClassByteArrayMap.containsKey(name)) {
                return new ByteArrayInputStream(javaClassByteArrayMap.get(name));
            }
            return super.getResourceAsStream(name);
        }

    }

    /**
     * 构造TestClassLoader，用于完成隔离测试
     *
     * @return TestClassLoader
     */
    protected TestClassLoader newTestClassLoader() {
        return new TestClassLoader();
    }

    protected Class<?> watchingWithNamespace(final String namespace,
                                             final Class<?> targetClass,
                                             final Filter filter,
                                             final EventListener listener,
                                             final Event.Type... eventType) throws IOException, InvocationTargetException, IllegalAccessException {
        final int listenerId = LISTENER_ID_SEQ.getAndIncrement();
        final TestClassLoader loader = newTestClassLoader();
        final CoreConfigure coreCfg = CoreConfigure.toConfigure(
                StringUtils.isBlank(namespace)
                        ? ""
                        : String.format(";namespace=%s;", namespace),
                ""
        );
        EventListenerHandlers.getSingleton().active(listenerId, listener, eventType);
        return loader.defineClass(
                targetClass.getName(),
                new EventEnhancer().toByteCodeArray(
                        loader,
                        toByteArray(targetClass),
                        new ExtFilterMatcher(ExtFilter.ExtFilterFactory.make(filter))
                                .matching(new ClassStructureImplByJDK(targetClass))
                                .getBehaviorSignCodes(),
                        coreCfg.getNamespace(),
                        listenerId,
                        eventType
                )//new
        );//return
    }

    protected Class<?> watching(final Class<?> targetClass,
                                final Filter filter,
                                final EventListener listener,
                                final Event.Type... eventType) throws IOException, InvocationTargetException, IllegalAccessException {
        return watchingWithNamespace(
                null,
                targetClass,
                filter,
                listener,
                eventType
        );
    }

}
