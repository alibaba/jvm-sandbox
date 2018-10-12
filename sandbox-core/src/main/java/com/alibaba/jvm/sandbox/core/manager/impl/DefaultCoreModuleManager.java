package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.*;
import com.alibaba.jvm.sandbox.api.resource.*;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.classloader.ModuleClassLoader;
import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus;
import com.alibaba.jvm.sandbox.core.manager.ProviderManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.jvm.sandbox.api.ModuleException.ErrorCode.*;
import static com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus.Event.LOAD_COMPLETED;
import static org.apache.commons.io.FileUtils.listFiles;

/**
 * 默认的模块管理实现
 * Created by luanjia on 16/10/4.
 */
public class DefaultCoreModuleManager implements CoreModuleManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Instrumentation inst;
    private final CoreLoadedClassDataSource classDataSource;
    private final CoreConfigure cfg;
    private final ClassLoader sandboxClassLoader;
    private final ModuleLifeCycleEventBus moduleLifeCycleEventBus;
    private final ProviderManager providerManager;

    // 模块目录&文件集合
    private final File[] moduleLibDirArray;

    // 已加载的模块集合
    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();

    /**
     * 模块模块管理
     *
     * @param inst                    inst
     * @param classDataSource         已加载类数据源
     * @param cfg                     模块核心配置
     * @param sandboxClassLoader      沙箱加载ClassLoader
     * @param moduleLifeCycleEventBus 模块生命周期通知总线
     * @param providerManager         服务提供者管理器
     */
    public DefaultCoreModuleManager(final Instrumentation inst,
                                    final CoreLoadedClassDataSource classDataSource,
                                    final CoreConfigure cfg,
                                    final ClassLoader sandboxClassLoader,
                                    final ModuleLifeCycleEventBus moduleLifeCycleEventBus,
                                    final ProviderManager providerManager) {
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.cfg = cfg;
        this.sandboxClassLoader = sandboxClassLoader;
        this.moduleLifeCycleEventBus = moduleLifeCycleEventBus;
        this.providerManager = providerManager;

        // 初始化模块目录
        this.moduleLibDirArray = mergeFileArray(
                new File[]{new File(cfg.getSystemModuleLibPath())},
                cfg.getUserModuleLibFilesWithCache()
        );

        // 初始化加载所有的模块
        try {
            reset();
        } catch (ModuleException e) {
            logger.warn("init module[id={};] occur error={}.", e.getUniqueId(), e.getErrorCode(), e);
        }
    }

    private File[] mergeFileArray(File[] aFileArray, File[] bFileArray) {
        final List<File> _r = new ArrayList<File>();
        for (final File aFile : aFileArray) {
            _r.add(aFile);
        }
        for (final File bFile : bFileArray) {
            _r.add(bFile);
        }
        return _r.toArray(new File[]{});
    }

    /*
     * 通知模块生命周期
     */
    private void fireModuleLifecycle(final CoreModule coreModule, final ModuleLifeCycleEventBus.Event e) throws ModuleException {
        if (coreModule.getModule() instanceof ModuleLifecycle) {
            final ModuleLifecycle moduleLifecycle = (ModuleLifecycle) coreModule.getModule();
            final String uniqueId = coreModule.getUniqueId();
            switch (e) {

                case LOAD: {
                    try {
                        moduleLifecycle.onLoad();
                    } catch (Throwable throwable) {
                        throw new ModuleException(uniqueId, MODULE_LOAD_ERROR, throwable);
                    }
                    break;
                }

                case UNLOAD: {
                    try {
                        moduleLifecycle.onUnload();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_UNLOAD_ERROR, throwable);
                    }
                    break;
                }

                case ACTIVE: {
                    try {
                        moduleLifecycle.onActive();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_ACTIVE_ERROR, throwable);
                    }
                    break;
                }

                case FROZE: {
                    try {
                        moduleLifecycle.onFrozen();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_FROZEN_ERROR, throwable);
                    }
                    break;
                }

            }// switch
        }

        if (e == LOAD_COMPLETED
                && coreModule.getModule() instanceof LoadCompleted) {
            final String uniqueId = coreModule.getUniqueId();
            final LoadCompleted loadCompleted = (LoadCompleted) coreModule.getModule();
            try {
                loadCompleted.loadCompleted();
            } catch (Throwable throwable) {
                logger.warn("module[id={}] occur error when load completed.", uniqueId, throwable);
            }
        }

        // fire the bus
        moduleLifeCycleEventBus.fire(coreModule, e);
    }


    private void injectRequiredResource(final CoreModule coreModule) throws IllegalAccessException {

        final Module module = coreModule.getModule();
        final Field[] resourceFieldArray = FieldUtils.getFieldsWithAnnotation(module.getClass(), Resource.class);
        if (ArrayUtils.isEmpty(resourceFieldArray)) {
            return;
        }

        for (final Field resourceField : resourceFieldArray) {
            final Class<?> fieldType = resourceField.getType();

            // LoadedClassDataSource对象注入
            if (LoadedClassDataSource.class.isAssignableFrom(fieldType)) {
                FieldUtils.writeField(resourceField, module, classDataSource, true);
            }

            // ModuleContactorManager对象注入
            else if (ModuleEventWatcher.class.isAssignableFrom(fieldType)) {
                final ModuleEventWatcher moduleEventWatcher = new DefaultModuleEventWatcher(
                        inst,
                        classDataSource,
                        coreModule,
                        cfg.isEnableUnsafe()
                );
                moduleLifeCycleEventBus.append((DefaultModuleEventWatcher) moduleEventWatcher);
                FieldUtils.writeField(resourceField, module, moduleEventWatcher, true);
            }

            // ModuleController对象注入
            else if (ModuleController.class.isAssignableFrom(fieldType)) {
                final ModuleController moduleController = new DefaultModuleController(coreModule, this);
                FieldUtils.writeField(resourceField, module, moduleController, true);
            }

            // ModuleManager对象注入
            else if (ModuleManager.class.isAssignableFrom(fieldType)) {
                final ModuleManager moduleManager = new DefaultModuleManager(this);
                FieldUtils.writeField(resourceField, module, moduleManager, true);
            }

            // ConfigInfo注入
            else if (ConfigInfo.class.isAssignableFrom(fieldType)) {
                final ConfigInfo configInfo = new DefaultConfigInfo(cfg);
                FieldUtils.writeField(resourceField, module, configInfo, true);
            }

            // EventMonitor注入
            else if (EventMonitor.class.isAssignableFrom(fieldType)) {
                FieldUtils.writeField(resourceField, module, new DefaultEventMonitor(), true);
            }

            // 其他情况需要输出日志警告
            else {
                logger.warn("inject required @Resource field[name={};] into module[id={};class={};] failed, type={}; was not support yet.",
                        resourceField.getName(), coreModule.getUniqueId(), module.getClass(), fieldType);
            }

        }

    }

    /**
     * 加载并注册模块
     * <p>1. 如果模块已经存在则返回已经加载过的模块</p>
     * <p>2. 如果模块不存在，则进行常规加载</p>
     * <p>3. 如果模块初始化失败，则抛出异常</p>
     *
     * @param uniqueId          模块ID
     * @param module            模块对象
     * @param moduleJarFile     模块所在JAR文件
     * @param moduleClassLoader 负责加载模块的ClassLoader
     * @throws ModuleException 加载模块失败
     */
    private synchronized void load(final String uniqueId,
                                   final Module module,
                                   final File moduleJarFile,
                                   final ModuleClassLoader moduleClassLoader) throws ModuleException {

        if (loadedModuleBOMap.containsKey(uniqueId)) {
            logger.info("module[id={};] already loaded, ignore this load.", uniqueId);
            return;
        }

        // 初始化模块信息
        final CoreModule coreModule = new CoreModule(uniqueId, moduleJarFile, moduleClassLoader, module);

        // 注入@Resource资源
        try {
            injectRequiredResource(coreModule);
        } catch (IllegalAccessException iae) {
            throw new ModuleException(uniqueId, MODULE_LOAD_ERROR, iae);
        }

        // 通知生命周期:模块加载开始
        fireModuleLifecycle(coreModule, ModuleLifeCycleEventBus.Event.LOAD);

        // 设置为已经加载
        coreModule.setLoaded(true);

        // 如果模块被标记为启动时激活，这里需要主动对模块进行一次激活操作
        final Information info = module.getClass().getAnnotation(Information.class);
        if (info.isActiveOnLoad()) {
            active(coreModule);
        }

        // 注册到模块列表中
        loadedModuleBOMap.put(uniqueId, coreModule);

        // 通知声明周期，模块加载完成
        fireModuleLifecycle(coreModule, LOAD_COMPLETED);

        logger.info("loaded module[id={};class={};] success, loader={}", uniqueId, module.getClass(), moduleClassLoader);

    }

    /**
     * 卸载并删除注册模块
     * <p>1. 如果模块原本就不存在，则幂等此次操作</p>
     * <p>2. 如果模块存在则尝试进行卸载</p>
     * <p>3. 卸载模块之前会尝试冻结该模块</p>
     *
     * @param coreModule 等待被卸载的模块
     * @param isForce    是否强制卸载
     * @throws ModuleException 卸载模块失败
     */
    @Override
    public synchronized CoreModule unload(final CoreModule coreModule,
                                          final boolean isForce) throws ModuleException {

        try {
            // 通知生命周期
            fireModuleLifecycle(coreModule, ModuleLifeCycleEventBus.Event.UNLOAD);
        } catch (ModuleException meCause) {

            if (isForce) {
                logger.warn("unload module[id={};class={};], occur error={}, but isForce=true, so ignore this failed.",
                        meCause.getUniqueId(),
                        coreModule.getModule().getClass(),
                        meCause.getErrorCode(),
                        meCause
                );
            } else {
                throw meCause;
            }

        }

        // 尝试冻结模块
        frozen(coreModule, isForce);

        // 从模块注册表中删除
        loadedModuleBOMap.remove(coreModule.getUniqueId());

        // 标记模块卸载
        coreModule.setLoaded(false);

        // 尝试关闭ClassLoader
        closeModuleClassLoaderIfNecessary(coreModule.getLoader());

        return coreModule;
    }

    @Override
    public synchronized void active(final CoreModule coreModule) throws ModuleException {

        // 如果模块已经被激活，则直接幂等返回
        if (coreModule.isActivated()) {
            return;
        }

        // 通知生命周期
        fireModuleLifecycle(coreModule, ModuleLifeCycleEventBus.Event.ACTIVE);

        // 激活所有监听器
        for (final ClassFileTransformer classFileTransformer : coreModule.getSandboxClassFileTransformers()) {
            if (!(classFileTransformer instanceof SandboxClassFileTransformer)) {
                continue;
            }
            final SandboxClassFileTransformer sandboxClassFileTransformer
                    = (SandboxClassFileTransformer) classFileTransformer;
            EventListenerHandlers.getSingleton().active(
                    sandboxClassFileTransformer.getListenerId(),
                    sandboxClassFileTransformer.getEventListener(),
                    sandboxClassFileTransformer.getEventTypeArray()
            );
        }

        coreModule.setActivated(true);
        logger.info("active module[id={};] finish.", coreModule.getUniqueId());
    }

    @Override
    public synchronized void frozen(final CoreModule coreModule,
                                    final boolean isForce) throws ModuleException {

        // 如果模块已经被冻结(尚未被激活)，则直接幂等返回
        if (!coreModule.isActivated()) {
            return;
        }

        try {

            // 通知生命周期
            fireModuleLifecycle(coreModule, ModuleLifeCycleEventBus.Event.FROZE);

        } catch (ModuleException meCause) {

            if (isForce) {
                logger.warn("frozen module[id={};class={};], occur error, but isForce=true, so ignore this failed.",
                        coreModule.getUniqueId(), coreModule.getModule().getClass(), meCause);
            } else {
                throw meCause;
            }

        }

        // 冻结所有监听器
        for (final ClassFileTransformer classFileTransformer : coreModule.getSandboxClassFileTransformers()) {
            if (!(classFileTransformer instanceof SandboxClassFileTransformer)) {
                continue;
            }
            final SandboxClassFileTransformer sandboxClassFileTransformer
                    = (SandboxClassFileTransformer) classFileTransformer;
            EventListenerHandlers.getSingleton()
                    .frozen(sandboxClassFileTransformer.getListenerId());
        }

        coreModule.setActivated(false);
        logger.info("frozen module[id={};] finish.", coreModule.getUniqueId());
    }

    @Override
    public Collection<CoreModule> list() {
        return loadedModuleBOMap.values();
    }

    @Override
    public CoreModule get(String uniqueId) {
        return loadedModuleBOMap.get(uniqueId);
    }

    @Override
    public CoreModule getThrowsExceptionIfNull(String uniqueId) throws ModuleException {
        final CoreModule coreModule = get(uniqueId);
        if (null == coreModule) {
            throw new ModuleException(uniqueId, MODULE_NOT_EXISTED);
        }
        return coreModule;
    }


    private static boolean isOptimisticDirectoryContainsFile(final File directory,
                                                             final File child) {
        try {
            return FileUtils.directoryContains(directory, child);
        } catch (IOException e) {
            // 如果这里能抛出异常，则说明directory或者child发生损坏
            // 需要返回TRUE以此作乐观推断，出错的情况也属于当前目录
            // 这个逻辑没毛病,主要是用来应对USER目录被删除引起IOException的情况
            return true;
        }
    }

    private boolean isSystemModule(final File child) {
        return isOptimisticDirectoryContainsFile(new File(cfg.getSystemModuleLibPath()), child);
    }

    /**
     * 用户模块文件加载回调
     */
    final private class InnerModuleJarLoadCallback implements ModuleJarLoader.ModuleJarLoadCallback {

        @Override
        public void onLoad(File moduleJarFile) throws Throwable {
            providerManager.loading(moduleJarFile);
        }

    }

    /**
     * 用户模块加载回调
     */
    final private class InnerModuleLoadCallback implements ModuleJarLoader.ModuleLoadCallback {
        @Override
        public void onLoad(final String uniqueId,
                           final Class moduleClass,
                           final Module module,
                           final File moduleJarFile,
                           final ModuleClassLoader moduleClassLoader) throws Throwable {

            // 如果之前已经加载过了相同ID的模块，则放弃当前模块的加载
            if (loadedModuleBOMap.containsKey(uniqueId)) {
                final CoreModule existedCoreModule = get(uniqueId);
                logger.info("module[id={};class={};loader={};] already loaded, ignore load this module. existed-module[id={};class={};loader={};]",
                        uniqueId, moduleClass, moduleClassLoader,
                        existedCoreModule.getUniqueId(), existedCoreModule.getModule().getClass(), existedCoreModule.getLoader());
                return;
            }

            // 需要经过ModuleLoadingChain的过滤
            providerManager.loading(
                    uniqueId,
                    moduleClass,
                    module,
                    moduleJarFile,
                    moduleClassLoader
            );

            // 之前没有加载过，这里进行加载
            logger.debug("found new module[id={};class={};loader={};], prepare to load.",
                    uniqueId, moduleClass, moduleClassLoader);
            load(uniqueId, module, moduleJarFile, moduleClassLoader);
        }
    }

    @Override
    public synchronized void flush(final boolean isForce) throws ModuleException {

        if (isForce) {
            forceFlush();
        } else {
            softFlush();
        }

    }

    @Override
    public synchronized void reset() throws ModuleException {

        // 1. 强制卸载所有模块
        for (final CoreModule coreModule : new ArrayList<CoreModule>(loadedModuleBOMap.values())) {
            unload(coreModule, true);
        }

        // 2. 加载所有模块
        for (final File moduleLibDir : moduleLibDirArray) {
            // 用户模块加载目录，加载用户模块目录下的所有模块
            // 对模块访问权限进行校验
            if (moduleLibDir.exists()
                    && moduleLibDir.canRead()) {
                new ModuleJarLoader(moduleLibDir, cfg.getLaunchMode(), sandboxClassLoader)
                        .load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
            } else {
                logger.warn("MODULE-LIB[{}] can not access, ignore flush load this lib.", moduleLibDir);
            }
        }

    }

    /**
     * 关闭Module的ClassLoader
     * 如ModuleClassLoader所加载上来的所有模块都已经被卸载，则该ClassLoader需要主动进行关闭
     *
     * @param loader 需要被关闭的ClassLoader
     */
    private void closeModuleClassLoaderIfNecessary(final ClassLoader loader) {

        if (!(loader instanceof ModuleClassLoader)) {
            return;
        }

        // 查找已经注册的模块中是否仍然还包含有ModuleClassLoader的引用
        boolean hasRef = false;
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            if (loader == coreModule.getLoader()) {
                hasRef = true;
                break;
            }
        }

        if (!hasRef) {
            ((ModuleClassLoader) loader).closeIfPossible();
            logger.info("all module unload, {} was release.", loader);
        }

    }


    private boolean isChecksumCRC32Existed(long checksumCRC32) {
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            if (coreModule.getLoader().getChecksumCRC32() == checksumCRC32) {
                return true;
            }
        }
        return false;
    }

    /**
     * 软刷新
     * 找出有变动的模块文件，有且仅有改变这些文件所对应的模块
     *
     * @throws ModuleException 模块操作失败
     */
    private void softFlush() throws ModuleException {

        final File systemModuleLibDir = new File(cfg.getSystemModuleLibPath());
        final File[] userModuleLibDirArray = cfg.getUserModuleLibFilesWithCache();
        for (final File userModuleLibDir : userModuleLibDirArray) {

            try {
                // final File userModuleLibDir = new File(cfg.getUserModuleLibPath());
                final ArrayList<File> appendJarFiles = new ArrayList<File>();
                final ArrayList<CoreModule> removeCoreModules = new ArrayList<CoreModule>();
                final ArrayList<Long> checksumCRC32s = new ArrayList<Long>();

                // 1. 找出所有有变动的文件(add/remove)
                for (final File jarFile : listFiles(userModuleLibDir, new String[]{"jar"}, false)) {
                    final long checksumCRC32;
                    try {
                        checksumCRC32 = FileUtils.checksumCRC32(jarFile);
                    } catch (IOException e) {
                        logger.warn("soft flush {} failed, ignore this file.", jarFile, e);
                        continue;
                    }
                    checksumCRC32s.add(checksumCRC32);
                    // 如果CRC32已经在已加载的模块集合中存在，则说明这个文件没有变动，忽略
                    if (isChecksumCRC32Existed(checksumCRC32)) {
                        continue;
                    }
                    appendJarFiles.add(jarFile);
                }

                // 2. 找出所有待卸载的已加载用户模块
                for (final CoreModule coreModule : loadedModuleBOMap.values()) {
                    final ModuleClassLoader moduleClassLoader = coreModule.getLoader();

                    // 如果是系统模块目录则跳过
                    if(isOptimisticDirectoryContainsFile(systemModuleLibDir, coreModule.getJarFile())) {
                        continue;
                    }
//
//                    // 如果不是用户模块目录，忽略
//                    if (!isOptimisticDirectoryContainsFile(userModuleLibDir, coreModule.getJarFile())) {
//                        continue;
//                    }
                    // 如果CRC32已经在这次待加载的集合中，则说明这个文件没有变动，忽略
                    if (checksumCRC32s.contains(moduleClassLoader.getChecksumCRC32())) {
                        continue;
                    }
                    removeCoreModules.add(coreModule);
                }

                // 3. 删除remove
                for (final CoreModule coreModule : removeCoreModules) {
                    unload(coreModule, true);
                }

                // 4. 加载add
                for (final File jarFile : appendJarFiles) {
                    new ModuleJarLoader(jarFile, cfg.getLaunchMode(), sandboxClassLoader)
                            .load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
                }
            } catch (Throwable cause) {
                logger.warn("flushing USER_LIB_MODULE[{}] failed.", userModuleLibDir, cause);
            }

        }

    }

    /**
     * 强制刷新
     * 对所有已经加载的用户模块进行强行卸载并重新加载
     *
     * @throws ModuleException 模块操作失败
     */
    private void forceFlush() throws ModuleException {

        // 1. 卸载模块
        // 等待卸载的模块集合
        final Collection<CoreModule> waitingUnloadCoreModules = new ArrayList<CoreModule>();

        // 找出所有USER的模块，所以这些模块都卸载了
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            // 如果判断是属于USER模块目录下的模块，则加入到待卸载模块集合，稍后统一进行卸载
            if (!isSystemModule(coreModule.getJarFile())) {
                waitingUnloadCoreModules.add(coreModule);
            }
        }

        // 强制卸载掉所有等待卸载的模块集合中的模块
        for (final CoreModule coreModule : waitingUnloadCoreModules) {
            unload(coreModule, true);
        }


        // 2. 加载模块
        // 用户模块加载目录，加载用户模块目录下的所有模块
        // 对模块访问权限进行校验
        // 用户模块目录
        final File[] userModuleLibFileArray = cfg.getUserModuleLibFiles();
        for (final File userModuleLibDir : userModuleLibFileArray) {
            if (userModuleLibDir.exists()
                    && userModuleLibDir.canRead()) {
                new ModuleJarLoader(userModuleLibDir, cfg.getLaunchMode(), sandboxClassLoader)
                        .load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
            } else {
                logger.warn("MODULE-LIB[{}] can not access, ignore flush load this lib.", userModuleLibDir);
            }
        }

    }

}
