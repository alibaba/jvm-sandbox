package com.alibaba.jvm.sandbox.qatest.core.manager;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleException;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus.Event;
import com.alibaba.jvm.sandbox.core.manager.ProviderManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultModuleLifeCycleEventBus;
import com.alibaba.jvm.sandbox.core.util.FeatureCodec;
import com.alibaba.jvm.sandbox.core.util.matcher.Matcher;
import com.alibaba.jvm.sandbox.qatest.core.util.SandboxModuleJarBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;
import java.util.jar.JarFile;

import static com.alibaba.jvm.sandbox.api.ModuleException.ErrorCode.MODULE_ACTIVE_ERROR;
import static java.io.File.createTempFile;

public class CoreModuleManagerTestCase {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Information(id = "broken-on-cinit")
    public static class BrokenOnCInitModule implements Module {
        static {
            if (true) {
                throw new RuntimeException("BROKEN-ON-CINIT");
            }
        }
    }

    @Information(id = "broken-on-load")
    public static class BrokenOnLoadModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void onLoad() throws Throwable {
            throw new IllegalAccessException("BROKEN-ON-LOAD");
        }
    }

    @Information(id = "broken-on-unload")
    public static class BrokenOnUnLoadModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void onUnload() {
            throw new RuntimeException("BROKEN-ON-UNLOAD");
        }
    }

    @Information(id = "broken-on-active")
    public static class BrokenOnActiveModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void onActive() {
            throw new RuntimeException("BROKEN-ON-ACTIVE");
        }
    }

    @Information(id = "broken-on-lazy-active", isActiveOnLoad = false)
    public static class BrokenOnLazyActiveModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void onActive() {
            throw new RuntimeException("BROKEN-ON-LAZY-ACTIVE");
        }
    }

    @Information(id = "broken-on-frozen")
    public static class BrokenOnFrozenModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void onFrozen() {
            throw new RuntimeException("BROKEN-ON-FROZEN");
        }
    }

    @Information(id = "broken-on-load-completed")
    public static class BrokenOnLoadCompletedModule extends ModuleLifeCycleAdapter implements Module {
        @Override
        public void loadCompleted() {
            throw new RuntimeException("BROKEN-ON-LOAD-COMPLETED");
        }
    }

    @Information(id = "normal-module")
    public static class NormalModule extends ModuleLifeCycleAdapter implements Module {

    }

    @Information(id = "normal-no-lazy-active-module", isActiveOnLoad = false)
    public static class NormalOnLazyActiveModule extends ModuleLifeCycleAdapter implements Module {

    }

    final class EmptyInstrumentation implements Instrumentation {

        @Override
        public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {

        }

        @Override
        public void addTransformer(ClassFileTransformer transformer) {

        }

        @Override
        public boolean removeTransformer(ClassFileTransformer transformer) {
            return false;
        }

        @Override
        public boolean isRetransformClassesSupported() {
            return false;
        }

        @Override
        public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {

        }

        @Override
        public boolean isRedefineClassesSupported() {
            return false;
        }

        @Override
        public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {

        }

        @Override
        public boolean isModifiableClass(Class<?> theClass) {
            return false;
        }

        @Override
        public Class[] getAllLoadedClasses() {
            return new Class[0];
        }

        @Override
        public Class[] getInitiatedClasses(ClassLoader loader) {
            return new Class[0];
        }

        @Override
        public long getObjectSize(Object objectToSize) {
            return 0;
        }

        @Override
        public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {

        }

        @Override
        public void appendToSystemClassLoaderSearch(JarFile jarfile) {

        }

        @Override
        public boolean isNativeMethodPrefixSupported() {
            return false;
        }

        @Override
        public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {

        }
    }

    final class EmptyCoreLoadedClassDataSource implements CoreLoadedClassDataSource {

        @Override
        public List<Class<?>> findForReTransform(Matcher matcher) {
            return null;
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
    }

    final class EmptyProviderManager implements ProviderManager {

        @Override
        public void loading(File moduleJarFile) throws Throwable {

        }

        @Override
        public void loading(String uniqueId, Class moduleClass, Module module, File moduleJarFile, ClassLoader moduleClassLoader) throws Throwable {

        }
    }

    private CoreConfigure buildingCoreConfigureWithUserModuleLib(final File... moduleJarFileArray) {

        final Set<String> moduleJarFilePathSet = new LinkedHashSet<String>();
        for (final File moduleJarFile : moduleJarFileArray) {
            moduleJarFilePathSet.add(moduleJarFile.getPath());
        }

        final Map<String, String> featureMap = new HashMap<String, String>();
        featureMap.put("user_module", StringUtils.join(moduleJarFilePathSet, ";"));
        featureMap.put("system_module", System.getProperty("user.home"));
        return CoreConfigure.toConfigure(
                new FeatureCodec(';', '=').toString(featureMap),
                null
        );
    }

    private File buildingModuleJarFileWithModuleClass(final File targetModuleJarFile,
                                                      final Class<? extends Module>... classOfModules) throws IOException {
        final SandboxModuleJarBuilder sandboxModuleJarBuilder = SandboxModuleJarBuilder.building(targetModuleJarFile);
        for (final Class<? extends Module> classOfModule : classOfModules) {
            sandboxModuleJarBuilder.putModuleClass(classOfModule);
        }
        return sandboxModuleJarBuilder.build();
    }


    @Test
    public void test$$CoreModuleManager$$ModuleLifeCycle() throws IOException, ModuleException {
        final File moduleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class
        );

        final Queue<Event> eventQueue = new LinkedList<Event>();
        eventQueue.offer(Event.LOAD);
        eventQueue.offer(Event.ACTIVE);
        eventQueue.offer(Event.LOAD_COMPLETED);

        final ModuleLifeCycleEventBus moduleLifeCycleEventBus = new DefaultModuleLifeCycleEventBus();
        moduleLifeCycleEventBus.append(new ModuleLifeCycleEventBus.ModuleLifeCycleEventListener() {
            @Override
            public boolean onFire(CoreModule coreModule, Event event) {
                Assert.assertEquals("normal-module", coreModule.getUniqueId());
                Assert.assertTrue(coreModule.getModule() instanceof Module);
                Assert.assertEquals(moduleJarFile, coreModule.getJarFile());
                if (eventQueue.peek() == event) {
                    eventQueue.poll();
                } else {
                    logger.warn("expect-event={} but actual-event={}", eventQueue.peek(), event);
                }
                return true;
            }
        });

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFile),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                moduleLifeCycleEventBus,
                new EmptyProviderManager()
        );
        coreModuleManager.reset();


        // 刚完成初始化，注册好的事件应该消化完成
        {
            Assert.assertEquals(1, coreModuleManager.list().size());
            Assert.assertEquals("normal-module", coreModuleManager.list().iterator().next().getUniqueId());
            Assert.assertTrue(eventQueue.isEmpty());
        }


        // 卸载模块
        {
            eventQueue.offer(Event.FROZE);
            eventQueue.offer(Event.UNLOAD);
            coreModuleManager.unload(coreModuleManager.get("normal-module"), false);
            Assert.assertEquals(0, coreModuleManager.list().size());
            Assert.assertTrue(eventQueue.isEmpty());
        }

        // 重新刷新
        {
            eventQueue.offer(Event.LOAD);
            eventQueue.offer(Event.ACTIVE);
            eventQueue.offer(Event.LOAD_COMPLETED);
            coreModuleManager.flush(false);
            Assert.assertEquals(1, coreModuleManager.list().size());
            Assert.assertEquals("normal-module", coreModuleManager.list().iterator().next().getUniqueId());
            Assert.assertTrue(eventQueue.isEmpty());
        }

        // 冻结-激活-冻结
        {
            eventQueue.offer(Event.FROZE);
            eventQueue.offer(Event.ACTIVE);
            eventQueue.offer(Event.FROZE);
            coreModuleManager.frozen(coreModuleManager.get("normal-module"), false);
            coreModuleManager.active(coreModuleManager.get("normal-module"));
            coreModuleManager.frozen(coreModuleManager.get("normal-module"), false);
            Assert.assertEquals(1, coreModuleManager.list().size());
            Assert.assertEquals("normal-module", coreModuleManager.list().iterator().next().getUniqueId());
            Assert.assertTrue(eventQueue.isEmpty());
        }


    }

    @Test
    public void test$$CoreModuleManager$$loading() throws IOException, ModuleException {

        final File moduleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class,
                BrokenOnCInitModule.class,
                BrokenOnLazyActiveModule.class,
                BrokenOnActiveModule.class,
                BrokenOnFrozenModule.class,
                BrokenOnLoadModule.class,
                BrokenOnUnLoadModule.class,
                BrokenOnLoadCompletedModule.class
        );

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFile),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                new DefaultModuleLifeCycleEventBus(),
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        final Set<String> uniqueIds = new LinkedHashSet<String>();
        for (final CoreModule coreModule : coreModuleManager.list()) {
            uniqueIds.add(coreModule.getUniqueId());
        }

        Assert.assertTrue(uniqueIds.contains("normal-module"));
        Assert.assertTrue(uniqueIds.contains("broken-on-unload"));
        Assert.assertTrue(uniqueIds.contains("broken-on-lazy-active"));
        Assert.assertTrue(uniqueIds.contains("broken-on-frozen"));
        Assert.assertTrue(uniqueIds.contains("broken-on-load-completed"));
        Assert.assertEquals(5, uniqueIds.size());

    }


    @Information(id = "another-normal-module")
    public static class AnotherNormalModule implements Module {

    }

    @Information(id = "another-normal-module")
    public static class ModifyAnotherNormalModule implements Module {

    }

    @Test
    public void test$$CoreModuleManager$$forceFlush() throws IOException, ModuleException {

        final File normalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class
        );

        final File anotherNormalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                AnotherNormalModule.class
        );

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(
                        normalModuleJarFile,
                        anotherNormalModuleJarFile
                ),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                new DefaultModuleLifeCycleEventBus(),
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        final Set<String> uniqueIds = new LinkedHashSet<String>();
        for (final CoreModule coreModule : coreModuleManager.list()) {
            uniqueIds.add(coreModule.getUniqueId());
        }

        Assert.assertTrue(uniqueIds.contains("normal-module"));
        Assert.assertTrue(uniqueIds.contains("another-normal-module"));
        Assert.assertEquals(2, uniqueIds.size());

        Assert.assertTrue(anotherNormalModuleJarFile.delete());
        coreModuleManager.flush(true);
        Assert.assertEquals(1, coreModuleManager.list().size());
        Assert.assertEquals("normal-module", coreModuleManager.list().iterator().next().getUniqueId());

    }

    @Test
    public void test$$CoreModuleManager$$softFlush$$delete() throws IOException, ModuleException {

        final File normalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class
        );

        final File anotherNormalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                AnotherNormalModule.class
        );

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(
                        normalModuleJarFile,
                        anotherNormalModuleJarFile
                ),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                new DefaultModuleLifeCycleEventBus(),
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        final Set<String> uniqueIds = new LinkedHashSet<String>();
        for (final CoreModule coreModule : coreModuleManager.list()) {
            uniqueIds.add(coreModule.getUniqueId());
        }

        Assert.assertTrue(uniqueIds.contains("normal-module"));
        Assert.assertTrue(uniqueIds.contains("another-normal-module"));
        Assert.assertEquals(2, uniqueIds.size());

        Assert.assertTrue(anotherNormalModuleJarFile.delete());
        coreModuleManager.flush(false);
        Assert.assertEquals(1, coreModuleManager.list().size());
        Assert.assertEquals("normal-module", coreModuleManager.list().iterator().next().getUniqueId());

    }


    @Test
    public void test$$CoreModuleManager$$softFlush$$modify() throws IOException, ModuleException {

        final File normalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class
        );

        final File anotherNormalModuleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                AnotherNormalModule.class
        );

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(
                        normalModuleJarFile,
                        anotherNormalModuleJarFile
                ),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                new DefaultModuleLifeCycleEventBus(),
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        {
            final Set<String> uniqueIds = new LinkedHashSet<String>();
            for (final CoreModule coreModule : coreModuleManager.list()) {
                uniqueIds.add(coreModule.getUniqueId());
            }

            Assert.assertTrue(uniqueIds.contains("normal-module"));
            Assert.assertTrue(uniqueIds.contains("another-normal-module"));
            Assert.assertEquals(2, uniqueIds.size());
        }

        {
            buildingModuleJarFileWithModuleClass(
                    anotherNormalModuleJarFile,
                    ModifyAnotherNormalModule.class
            );
            coreModuleManager.flush(false);

            final Set<String> uniqueIds = new LinkedHashSet<String>();
            for (final CoreModule coreModule : coreModuleManager.list()) {
                uniqueIds.add(coreModule.getUniqueId());
            }

            Assert.assertTrue(uniqueIds.contains("normal-module"));
            Assert.assertTrue(uniqueIds.contains("another-normal-module"));
            Assert.assertEquals(2, coreModuleManager.list().size());
            Assert.assertEquals(
                    ModifyAnotherNormalModule.class.getName(),
                    coreModuleManager.get("another-normal-module").getModule().getClass().getName()
            );
        }

    }


    @Test(expected = ModuleException.class)
    public void test$$CoreModuleManager$$getThrowsExceptionIfNull() throws IOException, ModuleException {

        final File moduleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalModule.class,
                BrokenOnCInitModule.class,
                BrokenOnLazyActiveModule.class,
                BrokenOnActiveModule.class,
                BrokenOnFrozenModule.class,
                BrokenOnLoadModule.class,
                BrokenOnUnLoadModule.class,
                BrokenOnLoadCompletedModule.class
        );

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFile),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                new DefaultModuleLifeCycleEventBus(),
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        coreModuleManager.getThrowsExceptionIfNull("not-existed-module");
    }

    @Test
    public void test$$CoreModuleManager$$activeOnSuccess() throws IOException, ModuleException {

        final File moduleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                NormalOnLazyActiveModule.class
        );


        final Queue<Event> eventQueue = new LinkedList<Event>();
        eventQueue.offer(Event.LOAD);
        eventQueue.offer(Event.LOAD_COMPLETED);

        final ModuleLifeCycleEventBus moduleLifeCycleEventBus = new DefaultModuleLifeCycleEventBus();
        moduleLifeCycleEventBus.append(new ModuleLifeCycleEventBus.ModuleLifeCycleEventListener() {
            @Override
            public boolean onFire(CoreModule coreModule, Event event) {
                Assert.assertEquals("normal-no-lazy-active-module", coreModule.getUniqueId());
                Assert.assertTrue(coreModule.getModule() instanceof Module);
                Assert.assertEquals(moduleJarFile, coreModule.getJarFile());
                if (eventQueue.peek() == event) {
                    eventQueue.poll();
                } else {
                    logger.warn("expect-event={} but actual-event={}", eventQueue.peek(), event);
                }
                return true;
            }
        });

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFile),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                moduleLifeCycleEventBus,
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        final CoreModule normalNoLazyActiveCoreModule = coreModuleManager
                .getThrowsExceptionIfNull("normal-no-lazy-active-module");

        {
            eventQueue.offer(Event.ACTIVE);
            coreModuleManager.active(normalNoLazyActiveCoreModule);
            Assert.assertTrue(eventQueue.isEmpty());
        }

        {
            eventQueue.offer(Event.FROZE);
            coreModuleManager.frozen(normalNoLazyActiveCoreModule, false);
            Assert.assertTrue(eventQueue.isEmpty());
        }

    }

    @Test
    public void test$$CoreModuleManager$$activeOnFailed() throws IOException, ModuleException {

        final File moduleJarFile = buildingModuleJarFileWithModuleClass(
                createTempFile("test-", ".jar"),
                BrokenOnLazyActiveModule.class
        );


        final Queue<Event> eventQueue = new LinkedList<Event>();
        eventQueue.offer(Event.LOAD);
        eventQueue.offer(Event.LOAD_COMPLETED);

        final ModuleLifeCycleEventBus moduleLifeCycleEventBus = new DefaultModuleLifeCycleEventBus();
        moduleLifeCycleEventBus.append(new ModuleLifeCycleEventBus.ModuleLifeCycleEventListener() {
            @Override
            public boolean onFire(CoreModule coreModule, Event event) {
                Assert.assertEquals("broken-on-lazy-active", coreModule.getUniqueId());
                Assert.assertTrue(coreModule.getModule() instanceof Module);
                Assert.assertEquals(moduleJarFile, coreModule.getJarFile());
                if (eventQueue.peek() == event) {
                    eventQueue.poll();
                } else {
                    logger.warn("expect-event={} but actual-event={}", eventQueue.peek(), event);
                }
                return true;
            }
        });

        final CoreModuleManager coreModuleManager = new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFile),
                new EmptyInstrumentation(),
                this.getClass().getClassLoader(),
                new EmptyCoreLoadedClassDataSource(),
                moduleLifeCycleEventBus,
                new EmptyProviderManager()
        );
        coreModuleManager.reset();

        final CoreModule brokenOnLazyActiveCoreModule = coreModuleManager
                .getThrowsExceptionIfNull("broken-on-lazy-active");

        {
            eventQueue.offer(Event.ACTIVE);
            try {
                coreModuleManager.active(brokenOnLazyActiveCoreModule);
            }catch (ModuleException me) {
                Assert.assertEquals("broken-on-lazy-active", me.getUniqueId());
                Assert.assertEquals(MODULE_ACTIVE_ERROR, me.getErrorCode());
            }

            Assert.assertFalse(eventQueue.isEmpty());
        }

    }


}
