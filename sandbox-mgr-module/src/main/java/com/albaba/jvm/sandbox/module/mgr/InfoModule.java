package com.albaba.jvm.sandbox.module.mgr;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.EventMonitor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 沙箱信息模块
 *
 * @author luanjia@taobao.com
 */
@Information(id = "info", version = "0.0.3", author = "luanjia@taobao.com")
public class InfoModule implements Module {

    @Resource
    private ConfigInfo configInfo;

    @Resource
    private EventMonitor eventMonitor;

    @Http("/version")
    public void version(final HttpServletResponse resp) throws IOException {

        final StringBuilder versionSB = new StringBuilder()
                .append("                    NAMESPACE : ").append(configInfo.getNamespace()).append("\n")
                .append("                      VERSION : ").append(configInfo.getVersion()).append("\n")
                .append("                         MODE : ").append(configInfo.getMode()).append("\n")
                .append("                  SERVER_ADDR : ").append(configInfo.getServerAddress().getHostName()).append("\n")
                .append("                  SERVER_PORT : ").append(configInfo.getServerAddress().getPort()).append("\n")
                .append("               UNSAFE_SUPPORT : ").append(configInfo.isEnableUnsafe() ? "ENABLE" : "DISABLE").append("\n")
                .append("                 SANDBOX_HOME : ").append(configInfo.getHome()).append("\n")
                .append("            SYSTEM_MODULE_LIB : ").append(configInfo.getSystemModuleLibPath()).append("\n")
                .append("              USER_MODULE_LIB : ").append(configInfo.getUserModuleLibPath()).append("\n")
                .append("          SYSTEM_PROVIDER_LIB : ").append(configInfo.getSystemProviderLibPath()).append("\n")
                .append("           EVENT_POOL_SUPPORT : ").append(configInfo.isEnableEventPool() ? "ENABLE" : "DISABLE");
                       /*############################# : */
        if (configInfo.isEnableEventPool()) {
            versionSB
                    .append("\n")
                           /*############################# : */
                    .append("  EVENT_POOL_PER_KEY_IDLE_MIN : ").append(configInfo.getEventPoolMinIdlePerEvent()).append("\n")
                    .append("  EVENT_POOL_PER_KEY_IDLE_MAX : ").append(configInfo.getEventPoolMaxIdlePerEvent()).append("\n")
                    .append(" EVENT_POOL_PER_KEY_TOTAL_MAX : ").append(configInfo.getEventPoolMaxTotalPerEvent()).append("\n")
                    .append("             EVENT_POOL_TOTAL : ").append(configInfo.getEventPoolMaxTotal())
            ;
        }

        resp.getWriter().println(versionSB.toString());

    }

    @Http("/event-pool")
    public void eventPool(final HttpServletResponse resp) throws IOException {

        for (Event.Type type : Event.Type.values()) {
            resp.getWriter().println(String.format(
                    "%18s : %d / %d",
                    type,
                    eventMonitor.getEventPoolInfo().getNumActive(type),
                    eventMonitor.getEventPoolInfo().getNumIdle(type)
            ));
        }

    }

}
