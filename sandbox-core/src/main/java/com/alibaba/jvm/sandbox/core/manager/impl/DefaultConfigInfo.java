package com.alibaba.jvm.sandbox.core.manager.impl;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.server.jetty.JettyCoreServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * 默认配置信息实现
 * Created by luanjia@taobao.com on 2017/2/9.
 */
public class DefaultConfigInfo implements ConfigInfo {

    private final CoreConfigure cfg;

    public DefaultConfigInfo(CoreConfigure cfg) {
        this.cfg = cfg;
    }

    @Override
    public Information.Mode getMode() {
        return cfg.getLaunchMode();
    }

    @Override
    public boolean isEnableUnsafe() {
        return cfg.isEnableUnsafe();
    }

    @Override
    public String getHome() {
        return cfg.getJvmSandboxHome();
    }

    @Override
    public String getModuleLibPath() {
        return getSystemModuleLibPath();
    }

    @Override
    public String getSystemModuleLibPath() {
        return cfg.getSystemModuleLibPath();
    }

    @Override
    public String getSystemProviderLibPath() {
        return cfg.getProviderLibPath();
    }

    @Override
    public String getUserModuleLibPath() {
        return cfg.getUserModuleLibPath();
    }

    @Override
    public boolean isEnableEventPool() {
        return cfg.isEventPoolEnable();
    }

    @Override
    public int getEventPoolKeyMin() {
        return cfg.getEventPoolKeyMin();
    }

    @Override
    public int getEventPoolKeyMax() {
        return cfg.getEventPoolKeyMax();
    }

    @Override
    public int getEventPoolTotal() {
        return cfg.getEventPoolTotal();
    }

    @Override
    public InetSocketAddress getServerAddress() {
        try {
            return JettyCoreServer.getInstance().getLocal();
        } catch (Throwable cause) {
            return new InetSocketAddress("0.0.0.0", 0);
        }
    }

    @Override
    public String getVersion() {
        final InputStream is = getClass().getResourceAsStream("/com/alibaba/jvm/sandbox/version");
        try {
            return IOUtils.toString(is);
        } catch (IOException e) {
            // impossible
            return "UNKNOW_VERSION";
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
