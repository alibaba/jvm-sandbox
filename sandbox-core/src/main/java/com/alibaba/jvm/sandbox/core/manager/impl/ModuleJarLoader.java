package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.routing.RoutingInfo;
import com.alibaba.jvm.sandbox.api.util.ClassloaderUtil;
import com.alibaba.jvm.sandbox.core.classloader.ModuleJarClassLoader;
import com.alibaba.jvm.sandbox.core.classloader.RoutingURLClassLoader;
import com.alibaba.jvm.sandbox.core.classloader.RoutingURLClassLoader.Routing;
import com.alibaba.jvm.sandbox.core.classloader.SpecialRoutingHandler;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

class ModuleJarLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 等待加载的模块jar文件
    private final File moduleJarFile;

    // 沙箱加载模式
    private final Information.Mode mode;

    private final CoreLoadedClassDataSource classDataSource;

    ModuleJarLoader(final File moduleJarFile,
                    final Information.Mode mode,
                    final CoreLoadedClassDataSource classDataSource) {
        this.moduleJarFile = moduleJarFile;
        this.mode = mode;
        this.classDataSource = classDataSource;
    }


    private boolean loadingModules(final ModuleJarClassLoader moduleClassLoader,
                                   final ModuleLoadCallback mCb) {

        final Set<String> loadedModuleUniqueIds = new LinkedHashSet<String>();
        final ServiceLoader<Module> moduleServiceLoader = ServiceLoader.load(Module.class, moduleClassLoader);
        final Iterator<Module> moduleIt = moduleServiceLoader.iterator();
        while (moduleIt.hasNext()) {

            final Module module;
            try {
                module = moduleIt.next();
            } catch (Throwable cause) {
                logger.warn("loading module instance failed: instance occur error, will be ignored. module-jar={}", moduleJarFile, cause);
                continue;
            }

            final Class<?> classOfModule = module.getClass();

            // 判断模块是否实现了@Information标记
            if (!classOfModule.isAnnotationPresent(Information.class)) {
                logger.warn("loading module instance failed: not implements @Information, will be ignored. class={};module-jar={};",
                        classOfModule,
                        moduleJarFile
                );
                continue;
            }

            final Information info = classOfModule.getAnnotation(Information.class);
            final String uniqueId = info.id();

            // 判断模块ID是否合法
            if (StringUtils.isBlank(uniqueId)) {
                logger.warn("loading module instance failed: @Information.id is missing, will be ignored. class={};module-jar={};",
                        classOfModule,
                        moduleJarFile
                );
                continue;
            }

            // 判断模块要求的启动模式和容器的启动模式是否匹配
            if (!ArrayUtils.contains(info.mode(), mode)) {
                logger.warn("loading module instance failed: launch-mode is not match module required, will be ignored. module={};launch-mode={};required-mode={};class={};module-jar={};",
                        uniqueId,
                        mode,
                        StringUtils.join(info.mode(), ","),
                        classOfModule,
                        moduleJarFile
                );
                continue;
            }

            try {
                if (null != mCb) {
                    mCb.onLoad(uniqueId, classOfModule, module, moduleJarFile, moduleClassLoader);
                }
            } catch (Throwable cause) {
                logger.warn("loading module instance failed: MODULE-LOADER-PROVIDER denied, will be ignored. module={};class={};module-jar={};",
                        uniqueId,
                        classOfModule,
                        moduleJarFile,
                        cause
                );
                continue;
            }

            loadedModuleUniqueIds.add(uniqueId);

        }


        logger.info("loaded module-jar completed, loaded {} module in module-jar={}, modules={}",
                loadedModuleUniqueIds.size(),
                moduleJarFile,
                loadedModuleUniqueIds
        );
        return !loadedModuleUniqueIds.isEmpty();
    }


    void load(final ModuleLoadCallback mCb) throws IOException {

        Routing[] specialRouting = null;
        try {
            specialRouting = getSpecialRouting();
        } catch (Throwable throwable) {
            // ignore or block ?
            logger.warn("get special routing occurred unexpected exception", throwable);
        }
        boolean hasModuleLoadedSuccessFlag = false;
        ModuleJarClassLoader moduleJarClassLoader = null;
        logger.info("prepare loading module-jar={};", moduleJarFile);
        try {
            moduleJarClassLoader = new ModuleJarClassLoader(moduleJarFile, specialRouting);

            final ClassLoader preTCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(moduleJarClassLoader);

            try {
                hasModuleLoadedSuccessFlag = loadingModules(moduleJarClassLoader, mCb);
            } finally {
                Thread.currentThread().setContextClassLoader(preTCL);
            }

        } finally {
            if (!hasModuleLoadedSuccessFlag
                    && null != moduleJarClassLoader) {
                logger.warn("loading module-jar completed, but NONE module loaded, will be close ModuleJarClassLoader. module-jar={};", moduleJarFile);
                moduleJarClassLoader.closeIfPossible();
            }
        }

    }


    Routing[] getSpecialRouting() throws IOException {
        final List<RoutingInfo> routingInfos = SpecialRoutingHandler.resolve(moduleJarFile);
        List<Routing> routingList = new ArrayList<Routing>(routingInfos.size());
        for (RoutingInfo routingInfo : routingInfos) {
            routingList.add(toRoutingRule(routingInfo));
        }
        return routingList.toArray(new Routing[0]);
    }

    /**
     * 转换成真正的路由表
     *
     * @param routingInfo 模块传递的路由
     * @return
     */
    private Routing toRoutingRule(final RoutingInfo routingInfo) {
        if (routingInfo == null) {
            return null;
        }
        Routing routing = null;
        switch (routingInfo.getType()) {
            case TARGET_CLASS:
                String className = routingInfo.getTargetClass();
                for (Class<?> clazz : classDataSource.list()) {
                    if (!(isSelfClassloader(clazz.getClassLoader())) && StringUtils.equals(className, clazz.getName())) {
                        logger.info("find target routing rule,class={},classloader={},pattern={}", className, clazz.getClassLoader(), routingInfo.getPattern());
                        routing = new Routing(clazz.getClassLoader(), routingInfo.getPattern());
                        break;
                    }
                }
                break;
            case TARGET_CLASS_LOADER:
                ClassLoader classLoader = routingInfo.getTargetClassloader();
                if (classLoader != null) {
                    logger.info("use target classloader routing rule,classloader={},pattern={}", classLoader, routingInfo.getPattern());
                    routing = new Routing(classLoader, routingInfo.getPattern());
                }
                break;
            case TARGET_CLASS_LOADER_NAME:
                String classloaderName = routingInfo.getTargetClassLoaderName();
                for (Class<?> clazz : classDataSource.list()) {
                    String targetName = ClassloaderUtil.wrapperName(clazz.getClassLoader());
                    if (clazz.getClassLoader() != null && StringUtils.equals(classloaderName, targetName)) {
                        logger.info("find target routing rule,classloaderName={},classloader={},pattern={}", classloaderName, clazz.getClassLoader(), routingInfo.getPattern());
                        routing = new Routing(clazz.getClassLoader(), routingInfo.getPattern());
                        break;
                    }
                }
                break;
            default:
                break;
        }
        if (routing == null) {
            logger.info("no suitable target classloader found,targetClass={},targetClassloader={},pattern={},",
                    routingInfo.getTargetClass(), routingInfo.getTargetClassLoaderName(), routingInfo.getPattern());
        }
        return routing;
    }


    private boolean isSelfClassloader(ClassLoader classLoader) {
        return  classLoader instanceof RoutingURLClassLoader
                || (classLoader != null && "SandboxClassLoader".equals(classLoader.getClass().getSimpleName()));
    }



    /**
     * 模块加载回调
     */
    public interface ModuleLoadCallback {

        /**
         * 模块加载回调
         *
         * @param uniqueId          模块ID
         * @param moduleClass       模块类
         * @param module            模块实例
         * @param moduleJarFile     模块所在Jar文件
         * @param moduleClassLoader 负责加载模块的ClassLoader
         * @throws Throwable 加载回调异常
         */
        void onLoad(String uniqueId,
                    Class moduleClass,
                    Module module,
                    File moduleJarFile,
                    ModuleJarClassLoader moduleClassLoader) throws Throwable;

    }

}
