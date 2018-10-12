package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.core.classloader.ModuleClassLoader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

import static org.apache.commons.io.FileUtils.convertFileCollectionToFileArray;
import static org.apache.commons.io.FileUtils.listFiles;

/**
 * 模块加载器
 * 用于从${module.lib}中加载所有的沙箱模块
 * Created by luanjia@taobao.com on 2016/11/17.
 */
public class ModuleJarLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 模块加载目录
    private final File moduleLibDir;

    // 沙箱加载模式
    private final Information.Mode mode;

    // 沙箱加载ClassLoader
    private final ClassLoader sandboxClassLoader;

    public ModuleJarLoader(final File moduleLibDir,
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
    public void load(final ModuleJarLoadCallback mjCb,
                     final ModuleLoadCallback mCb) {

        // 查找所有可加载的Jar文件
        final File[] moduleJarFileArray = toModuleJarFileArray();
        Arrays.sort(moduleJarFileArray);
        logger.info("module loaded found {} jar in {}", moduleJarFileArray.length, moduleLibDir);

        // 开始逐条加载
        for (final File moduleJarFile : moduleJarFileArray) {

            ModuleClassLoader moduleClassLoader = null;

            try {

                // 是否有模块加载成功
                boolean hasModuleLoadedSuccessFlag = false;

                if (null != mjCb) {
                    mjCb.onLoad(moduleJarFile);
                }

                // 模块ClassLoader
                moduleClassLoader = new ModuleClassLoader(moduleJarFile, sandboxClassLoader);

                final ClassLoader preTCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(moduleClassLoader);

                try {

                    final ServiceLoader<Module> moduleServiceLoader = ServiceLoader.load(Module.class, moduleClassLoader);
                    final Iterator<Module> moduleIt = moduleServiceLoader.iterator();
                    while (moduleIt.hasNext()) {

                        final Module module;
                        try {
                            module = moduleIt.next();
                        } catch (Throwable cause) {
                            logger.warn("module SPI new instance failed, ignore this SPI.", cause);
                            continue;
                        }

                        final Class<?> classOfModule = module.getClass();

                        if (!classOfModule.isAnnotationPresent(Information.class)) {
                            logger.info("{} was not implements @Information, ignore this SPI.", classOfModule);
                            continue;
                        }

                        final Information info = classOfModule.getAnnotation(Information.class);
                        if (StringUtils.isBlank(info.id())) {
                            logger.info("{} was not implements @Information[id], ignore this SPI.", classOfModule);
                            continue;
                        }

                        final String uniqueId = info.id();
                        if (!ArrayUtils.contains(info.mode(), mode)) {
                            logger.info("module[id={};class={};mode={};] was not matched sandbox launch mode : {}, ignore this SPI.",
                                    uniqueId, classOfModule, Arrays.asList(info.mode()), mode);
                            continue;
                        }

                        try {
                            if (null != mCb) {
                                mCb.onLoad(
                                        uniqueId, classOfModule, module, moduleJarFile,
                                        moduleClassLoader
                                );
                            }
                            hasModuleLoadedSuccessFlag = true;
                        } catch (Throwable cause) {
                            logger.warn("load module[id={};class={};] from JAR[file={};] failed, ignore this module.",
                                    uniqueId, classOfModule, moduleJarFile, cause);
                        }

                    }//while

                    if (!hasModuleLoadedSuccessFlag) {
                        logger.warn("load sandbox module JAR[file={}], but none module loaded, close this ModuleClassLoader.", moduleJarFile);
                        moduleClassLoader.closeIfPossible();
                    }

                } finally {
                    Thread.currentThread().setContextClassLoader(preTCL);
                }

            } catch (Throwable cause) {
                logger.warn("load sandbox module JAR[file={}] failed.", moduleJarFile, cause);
                if (null != moduleClassLoader) {
                    moduleClassLoader.closeIfPossible();
                }
            }

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
        void onLoad(final String uniqueId, final Class moduleClass, final Module module, final File moduleJarFile,
                    final ModuleClassLoader moduleClassLoader) throws Throwable;

    }

}
