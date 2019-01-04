package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.spi.ModuleJarLifeCycleProvider;
import com.alibaba.jvm.sandbox.core.classloader.ModuleClassLoader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.apache.commons.io.FileUtils.convertFileCollectionToFileArray;
import static org.apache.commons.io.FileUtils.listFiles;

/**
 * 模块加载器
 * 用于从${module.lib}中加载所有的沙箱模块
 * Created by luanjia@taobao.com on 2016/11/17.
 */
class ModuleJarLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 模块加载目录
    private final File moduleLibDir;

    // 沙箱加载模式
    private final Information.Mode mode;

    // 沙箱加载ClassLoader
    private final ClassLoader sandboxClassLoader;

    ModuleJarLoader(final File moduleLibDir,
                    final Information.Mode mode,
                    final ClassLoader sandboxClassLoader) {
        this.moduleLibDir = moduleLibDir;
        this.mode = mode;
        this.sandboxClassLoader = sandboxClassLoader;
    }

    private File[] toModuleJarFileArray() {
        if (moduleLibDir.exists()
                && moduleLibDir.isFile()
                && moduleLibDir.canRead()
                && StringUtils.endsWith(moduleLibDir.getName(), ".jar")) {
            return new File[]{
                    moduleLibDir
            };
        } else {
            return convertFileCollectionToFileArray(
                    listFiles(moduleLibDir, new String[]{"jar"}, false)
            );
        }
    }

    /**
     * 加载Module
     *
     * @param mjCb 模块文件加载回调
     * @param mCb  模块加载回掉
     */
    void load(final ModuleJarLoadCallback mjCb,
              final ModuleLoadCallback mCb) {

        // 查找所有可加载的Jar文件
        final File[] moduleJarFileArray = toModuleJarFileArray();
        Arrays.sort(moduleJarFileArray);

        // 记录下都有那些模块文件被找到即将被加载
        if (logger.isInfoEnabled()) {
            final Set<String> moduleJarFileNameSet = new LinkedHashSet<String>();
            for (final File moduleJarFile : moduleJarFileArray) {
                moduleJarFileNameSet.add(moduleJarFile.getName());
            }
            logger.info("loading module-lib={}, found {} module-jar files : {}",
                    moduleLibDir,
                    moduleJarFileArray.length,
                    moduleJarFileNameSet
            );
        }

        // 开始逐条加载
        for (final File moduleJarFile : moduleJarFileArray) {

            logger.info("loading module-jar={} from module-lib={};", moduleJarFile, moduleLibDir);

            // 是否有模块加载成功
            boolean hasModuleLoadedSuccessFlag = false;

            // 已加载的模块ID集合
            final Set<String> loadedModuleUniqueIds = new LinkedHashSet<String>();

            // MODULE-JAR-CLASSLOADER
            ModuleClassLoader moduleClassLoader = null;

            try {

                if (null != mjCb) {
                    try {
                        mjCb.onLoad(moduleJarFile);
                    } catch (Throwable cause) {
                        logger.warn("loading module-jar failed: JAR-LOADER-PROVIDER denied, will be ignore. module-jar={};", moduleJarFile, cause);
                        continue;
                    }
                }

                // 模块ClassLoader
                moduleClassLoader = new ModuleClassLoader(moduleJarFile, sandboxClassLoader);

                final ClassLoader preTCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(moduleClassLoader);

                // 模块Jar加载前回调


                try {

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
                        hasModuleLoadedSuccessFlag = true;

                    }//while

                } finally {
                    Thread.currentThread().setContextClassLoader(preTCL);
                }// try-catch for load module from Jar

            } catch (Throwable cause) {
                logger.info("load module-jar occur error, will be ignored. module-jar={};",
                        moduleJarFile,
                        cause
                );
            } finally {
                if (hasModuleLoadedSuccessFlag) {
                    logger.info("loaded module-jar completed, loaded {} module in module-jar={}, modules={}",
                            loadedModuleUniqueIds.size(),
                            moduleJarFile,
                            loadedModuleUniqueIds
                    );
                } else {
                    logger.warn("loaded module-jar completed, NONE module loaded, will be close ModuleClassLoader. module-jar={};", moduleJarFile, moduleClassLoader);
                    if (null != moduleClassLoader) {
                        moduleClassLoader.closeIfPossible();
                    }
                }
            }// try-catch for load jar from lib

        }

    }


    /**
     * 模块文件加载回调
     */
    public interface ModuleJarLoadCallback {

        /**
         * 模块文件加载回调
         *
         * @param moduleJarFile 模块文件
         * @throws Throwable 加载回调异常
         */
        void onLoad(File moduleJarFile) throws Throwable;

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
                    ModuleClassLoader moduleClassLoader) throws Throwable;

    }

}
