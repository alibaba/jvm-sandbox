package com.albaba.jvm.sandbox.module.mgr;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 沙箱信息模块
 * Created by luanjia@taobao.com on 2017/2/9.
 */
@Information(id = "info", version = "0.0.0.1", author = "luanjia@taobao.com")
public class InfoModule implements Module {

    @Resource
    private ConfigInfo configInfo;

    @Http("/version")
    public void version(final HttpServletResponse resp) throws IOException {

        final StringBuilder versionSB = new StringBuilder()
                .append("            VERSION : ").append(configInfo.getVersion()).append("\n")
                .append("               MODE : ").append(configInfo.getMode()).append("\n")
                .append("        SERVER_ADDR : ").append(configInfo.getServerAddress().getHostName()).append("\n")
                .append("        SERVER_PORT : ").append(configInfo.getServerAddress().getPort()).append("\n")
                .append("     UNSAFE_SUPPORT : ").append(configInfo.isEnableUnsafe() ? "ENABLE" : "DISABLE").append("\n")
                .append("       SANDBOX_HOME : ").append(configInfo.getHome()).append("\n")
                .append("  SYSTEM_MODULE_LIB : ").append(configInfo.getSystemModuleLibPath()).append("\n")
                .append("    USER_MODULE_LIB : ").append(configInfo.getUserModuleLibPath()).append("\n")
                .append("SYSTEM_PROVIDER_LIB : ").append(configInfo.getSystemProviderLibPath()).append("\n")
                .append(" EVENT_POOL_SUPPORT : ").append(configInfo.isEnableEventPool() ? "ENABLE" : "DISABLE");
                       /*################### : */
        if (configInfo.isEnableEventPool()) {
            versionSB
                    .append("\n")
                           /*################### : */
                    .append(" EVENT_POOL_KEY_MIN : ").append(configInfo.getEventPoolKeyMin()).append("\n")
                    .append(" EVENT_POOL_KEY_MAX : ").append(configInfo.getEventPoolKeyMax()).append("\n")
                    .append("   EVENT_POOL_TOTAL : ").append(configInfo.getEventPoolTotal())
            ;
        }

        resp.getWriter().println(versionSB.toString());

    }

}
