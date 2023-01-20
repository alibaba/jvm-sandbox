package com.alibaba.jvm.sandbox.core.classloader;

import com.alibaba.jvm.sandbox.api.annotation.Stealth;
import com.alibaba.jvm.sandbox.api.spi.ModuleJarUnLoadSpi;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.*;

import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.getJavaClassName;
import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.*;

/**
 * 模块类加载器
 *
 * @author luanjia@taobao.com
 */
@Stealth
public class ModuleJarClassLoader extends RoutingURLClassLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File moduleJarFile;
    private final File tempModuleJarFile;
    private final long checksumCRC32;

    /**
     * 复制要加载的Jar文件为临时文件，这样做的目的是为了规避Jar文件在加载和使用过程中被破坏
     *
     * @param moduleJarFile 模块Jar文件
     * @return 复制的临时文件
     * @throws IOException 复制文件失败
     */
    private static File copyToTempFile(final File moduleJarFile) throws IOException {
        File tempFile = File.createTempFile("sandbox_module_jar_", ".jar");
        tempFile.deleteOnExit();
        FileUtils.copyFile(moduleJarFile, tempFile);
        return tempFile;
    }

    public ModuleJarClassLoader(final File moduleJarFile,
                                final Routing... specialRouting) throws IOException {
        this(moduleJarFile, copyToTempFile(moduleJarFile), specialRouting);
    }

    private ModuleJarClassLoader(final File moduleJarFile,
                                 final File tempModuleJarFile,
                                 final Routing... specialRouting) throws IOException {
        super(
                new URL[]{new URL("file:" + tempModuleJarFile.getPath())},
                assembleRouting(new Routing(
                        ModuleJarClassLoader.class.getClassLoader(),
                        "^com\\.alibaba\\.jvm\\.sandbox\\.api\\..*$",
                        "^javax\\.servlet\\..*$",
                        "^javax\\.annotation\\.Resource.*$"
                ), specialRouting)
        );
        this.checksumCRC32 = FileUtils.checksumCRC32(moduleJarFile);
        this.moduleJarFile = moduleJarFile;
        this.tempModuleJarFile = tempModuleJarFile;

        try {
            cleanProtectionDomainWhichCameFromModuleJarClassLoader();
            logger.debug("clean ProtectionDomain in {}'s acc success.", this);
        } catch (Throwable e) {
            logger.warn("clean ProtectionDomain in {}'s acc failed.", this, e);
        }

    }

    /**
     * 合并路由信息
     *
     * @param selfRouting    自身路由表
     * @param specialRouting 扩展路由表
     * @return 合并完成路由信息
     */
    private static Routing[] assembleRouting(final Routing selfRouting, final Routing... specialRouting) {
        final List<Routing> rs = new ArrayList<>();
        if (specialRouting != null && specialRouting.length > 0) {
            rs.addAll(Arrays.asList(specialRouting));
        }
        rs.add(selfRouting);
        return rs.toArray(new Routing[0]);
    }

    /**
     * 清理来自URLClassLoader.acc.ProtectionDomain[]中，来自上一个ModuleJarClassLoader的ProtectionDomain
     * 这样写好蛋疼，而且还有不兼容的风险，从JDK6+都必须要这样清理，但我找不出更好的办法。
     * 在重置沙箱时，遇到MgrModule模块无法正确卸载类的情况，主要的原因是在于URLClassLoader.acc.ProtectionDomain[]中包含了上一个ModuleJarClassLoader的引用
     * 所以必须要在这里清理掉，否则随着重置次数的增加，类会越累积越多
     */
    private void cleanProtectionDomainWhichCameFromModuleJarClassLoader() {

        // got ProtectionDomain[] from URLClassLoader's acc
        final AccessControlContext acc = unCaughtGetClassDeclaredJavaFieldValue(URLClassLoader.class, "acc", this);
        final ProtectionDomain[] protectionDomainArray = unCaughtInvokeMethod(
                unCaughtGetClassDeclaredJavaMethod(AccessControlContext.class, "getContext"),
                acc
        );

        // remove ProtectionDomain which loader is ModuleJarClassLoader
        final Set<ProtectionDomain> cleanProtectionDomainSet = new LinkedHashSet<>();
        if (ArrayUtils.isNotEmpty(protectionDomainArray)) {
            for (final ProtectionDomain protectionDomain : protectionDomainArray) {
                if (protectionDomain.getClassLoader() == null
                        || !StringUtils.equals(ModuleJarClassLoader.class.getName(), protectionDomain.getClassLoader().getClass().getName())) {
                    cleanProtectionDomainSet.add(protectionDomain);
                }
            }
        }

        // rewrite acc
        final AccessControlContext newAcc = new AccessControlContext(cleanProtectionDomainSet.toArray(new ProtectionDomain[]{}));
        unCaughtSetClassDeclaredJavaFieldValue(URLClassLoader.class, "acc", this, newAcc);

    }

    private void onJarUnLoadCompleted() {
        try {
            final ServiceLoader<ModuleJarUnLoadSpi> moduleJarUnLoadSpiServiceLoader
                    = ServiceLoader.load(ModuleJarUnLoadSpi.class, this);
            for (final ModuleJarUnLoadSpi moduleJarUnLoadSpi : moduleJarUnLoadSpiServiceLoader) {
                logger.info("unloading module-jar: onJarUnLoadCompleted() loader={};moduleJarUnLoadSpi={};",
                        this,
                        getJavaClassName(moduleJarUnLoadSpi.getClass())
                );
                moduleJarUnLoadSpi.onJarUnLoadCompleted();
            }
        } catch (Throwable cause) {
            logger.warn("unloading module-jar: onJarUnLoadCompleted() occur error! loader={};", this, cause);
        }
    }

    public void closeIfPossible() {
        onJarUnLoadCompleted();

        // 关闭ClassLoader，释放资源
        try {
            ((Closeable) this).close();
        } catch (Throwable cause) {
            logger.warn("close ModuleJarClassLoader[file={}] failed. JDK7+", moduleJarFile, cause);
        } finally {

            // 在这里删除掉临时文件
            FileUtils.deleteQuietly(tempModuleJarFile);

        }

    }

    @Override
    public String toString() {
        return String.format("ModuleJarClassLoader[crc32=%s;file=%s;]", checksumCRC32, moduleJarFile);
    }

    public long getChecksumCRC32() {
        return checksumCRC32;
    }

}
