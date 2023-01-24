package com.alibaba.jvm.sandbox.core;

import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultProviderManager;
import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * 沙箱
 */
public class JvmSandbox {

    /**
     * 需要提前加载的sandbox工具类
     */
    private final static List<String> earlyLoadSandboxClassNameList = new ArrayList<>();

    static {
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.SandboxClassUtils");
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByAsm");
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandler");
    }

    private final CoreConfigure cfg;
    private final CoreModuleManager coreModuleManager;

    // 判断是否支持native
    private boolean isNativeSupported(Instrumentation inst) {

        // 当前经过测试的最高版本是：oracle-jdk-[1.8,12]
        final String javaSpecVersion = System.getProperty("java.specification.version");
        final boolean isSupportedJavaSpecVersion = StringUtils.isNotBlank(javaSpecVersion)
                && NumberUtils.toFloat(javaSpecVersion, 999f) <= 12f
                && NumberUtils.toFloat(javaSpecVersion, -1f) >= 1.8f;

        // 最终判断是否启用Native
        return isSupportedJavaSpecVersion
                && inst.isNativeMethodPrefixSupported();
    }

    public JvmSandbox(final CoreConfigure cfg,
                      final Instrumentation inst) {
        this.cfg = cfg;

        // 是否支持Native方法增强
        cfg.setNativeSupported(isNativeSupported(inst));

        this.coreModuleManager = SandboxProtector.instance.protectProxy(CoreModuleManager.class, new DefaultCoreModuleManager(
                cfg,
                inst,
                new DefaultCoreLoadedClassDataSource(inst, cfg.isEnableUnsafe(), cfg.isNativeSupported()),
                new DefaultProviderManager(cfg)
        ));
        init();
    }

    private void init() {
        doEarlyLoadSandboxClass();
        SpyUtils.init(cfg.getNamespace());
    }

    /**
     * 提前加载某些必要的类
     */
    private void doEarlyLoadSandboxClass() {
        for (String className : earlyLoadSandboxClassNameList) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                //加载sandbox内部的类，不可能加载不到
            }
        }
    }

    /**
     * 获取模块管理器
     *
     * @return 模块管理器
     */
    public CoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    /**
     * 销毁沙箱
     */
    public void destroy() {

        // 卸载所有的模块
        coreModuleManager.unloadAll();

        // 清理Spy
        SpyUtils.clean(cfg.getNamespace());

    }

}
