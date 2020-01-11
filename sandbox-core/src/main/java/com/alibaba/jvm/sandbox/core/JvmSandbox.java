package com.alibaba.jvm.sandbox.core;

import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandler;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultProviderManager;
import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;

import java.lang.instrument.Instrumentation;

/**
 * 沙箱
 */
public class JvmSandbox {

    private final CoreConfigure cfg;
    private final CoreModuleManager coreModuleManager;

    public JvmSandbox(final CoreConfigure cfg,
                      final Instrumentation inst) {
        EventListenerHandler.getSingleton();
        this.cfg = cfg;
        this.coreModuleManager = SandboxProtector.instance.protectProxy(CoreModuleManager.class, new DefaultCoreModuleManager(
                cfg,
                inst,
                new DefaultCoreLoadedClassDataSource(inst, cfg.isEnableUnsafe()),
                new DefaultProviderManager(cfg)
        ));

        init();
    }

    private void init() {
        SpyUtils.init(cfg.getNamespace());
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
