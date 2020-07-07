package com.alibaba.jvm.sandbox.core;

import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandler;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultProviderManager;
import com.alibaba.jvm.sandbox.core.util.SandboxProtector;
import com.alibaba.jvm.sandbox.core.util.SpyUtils;

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
    private final static List<String> earlyLoadSandboxClassNameList = new ArrayList<String>();

    static {
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.SandboxClassUtils");
        earlyLoadSandboxClassNameList.add("com.alibaba.jvm.sandbox.core.util.matcher.structure.ClassStructureImplByAsm");
    }

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
        doEarlyLoadSandboxClass();
        SpyUtils.init(cfg.getNamespace());
    }

    /**
     * 提前加载某些必要的类
     */
    private void doEarlyLoadSandboxClass() {
        for(String className : earlyLoadSandboxClassNameList){
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
