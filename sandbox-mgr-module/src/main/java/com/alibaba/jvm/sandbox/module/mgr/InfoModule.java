package com.alibaba.jvm.sandbox.module.mgr;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 沙箱信息模块
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "sandbox-info", version = "0.0.4", author = "luanjia@taobao.com")
public class InfoModule implements Module {

    @Resource
    private ConfigInfo configInfo;

    @Command("version")
    public void version(final PrintWriter writer) throws IOException {
        String versionSB =
                "                    NAMESPACE : " + configInfo.getNamespace() + "\n" +
                "                      VERSION : " + configInfo.getVersion() + "\n" +
                "                         MODE : " + configInfo.getMode() + "\n" +
                "                  SERVER_ADDR : " + configInfo.getServerAddress().getHostName() + "\n" +
                "                  SERVER_PORT : " + configInfo.getServerAddress().getPort() + "\n" +
                "               SERVER_CHARSET : " + configInfo.getServerCharset().toUpperCase() + "\n" +
                "               UNSAFE_SUPPORT : " + (configInfo.isEnableUnsafe() ? "ENABLE" : "DISABLE") + "\n" +
                "               NATIVE_SUPPORT : " + (configInfo.isNativeSupported() ? "ENABLE" : "DISABLE") + "\n" +
                "                 SANDBOX_HOME : " + configInfo.getHome() + "\n" +
                "            SYSTEM_MODULE_LIB : " + configInfo.getSystemModuleLibPath() + "\n" +
                "              USER_MODULE_LIB : " + StringUtils.join(configInfo.getUserModuleLibPaths(),";") + "\n" +
                "          SYSTEM_PROVIDER_LIB : " + configInfo.getSystemProviderLibPath() + "\n" +
                "           EVENT_POOL_SUPPORT : " + (configInfo.isEnableEventPool() ? "ENABLE" : "DISABLE");
        writer.println(versionSB);
        writer.flush();
    }

    //@Http("/event-pool")
    @Command("event-pool")
    @Deprecated
    public void eventPool(final PrintWriter writer) throws IOException {
        for (Event.Type type : Event.Type.values()) {
            writer.println(String.format("%18s : %d / %d", type, 0, 0));
        }
    }

}
