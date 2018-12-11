package com.alibaba.jvm.sandbox.module.mgr;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleException;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.resource.ModuleManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.matching;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 沙箱模块管理模块
 *
 * @author luanjia@taobao.com
 */
@Information(id = "module-mgr", author = "luanjia@taobao.com", version = "0.0.1")
public class ModuleMgrModule implements Module {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ModuleManager moduleManager;

    // 获取参数值
    private String getParamWithDefault(final HttpServletRequest req, final String name, final String defaultValue) {
        final String valueFromReq = req.getParameter(name);
        return StringUtils.isBlank(valueFromReq)
                ? defaultValue
                : valueFromReq;
    }

    // 搜索模块
    private Collection<Module> search(final String idsStringPattern) {
        final Collection<Module> foundModules = new ArrayList<Module>();
        for (Module module : moduleManager.list()) {
            final Information moduleInfo = module.getClass().getAnnotation(Information.class);
            if (!matching(moduleInfo.id(), idsStringPattern)) {
                continue;
            }
            foundModules.add(module);
        }
        return foundModules;
    }

    // 输出信息到客户端
    private void output(final PrintWriter writer, final String format, final Object... objectArray) {
        writer.println(String.format(format, objectArray));
    }

    @Http("/list")
    public void list(final HttpServletResponse resp) throws IOException {

        int total = 0;
        final PrintWriter writer = resp.getWriter();
        for (final Module module : moduleManager.list()) {

            final Information info = module.getClass().getAnnotation(Information.class);

            try {
                final boolean isActivated = moduleManager.isActivated(info.id());
                final boolean isLoaded = moduleManager.isLoaded(info.id());
                final int cCnt = moduleManager.cCnt(info.id());
                final int mCnt = moduleManager.mCnt(info.id());

                // 找到模块计数
                total++;

                //|id|isActivated|isLoaded|cCnt|mCnt|version|author|
                //|################|########|########|#######|#######|############|
                output(writer, "%-20s\t%-8s\t%-8s\t%-5s\t%-5s\t%-15s\t%s",
                        info.id(),
                        isActivated ? "ACTIVE" : "FROZEN",
                        isLoaded ? "LOADED" : "UNLOADED",
                        cCnt,
                        mCnt,
                        info.version(),
                        info.author()
                );

            } catch (ModuleException me) {
                logger.warn("get module info occur error when list modules, module[id={};class={};], error={}, ignore this module.",
                        me.getUniqueId(), module.getClass(), me.getErrorCode(), me);
            }

        }

        output(writer, "total=%s", total);
    }

    @Http("/flush")
    public void flush(final HttpServletRequest req,
                      final HttpServletResponse resp) throws ModuleException, IOException {
        final String isForceString = getParamWithDefault(req, "force", EMPTY);
        final boolean isForce = BooleanUtils.toBoolean(isForceString);
        moduleManager.flush(isForce);
        output(resp.getWriter(), "module flush finished, total=%s;", moduleManager.list().size());
    }

    @Http("/reset")
    public void reset(final HttpServletResponse resp) throws ModuleException, IOException {
        moduleManager.reset();
        output(resp.getWriter(), "module reset finished, total=%s;", moduleManager.list().size());
    }

    @Http("/unload")
    public void unload(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException, IOException {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(req, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            try {
                moduleManager.unload(info.id());
                total++;
            } catch (ModuleException me) {
                logger.warn("unload module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
            }
        }
        output(resp.getWriter(), "total %s module unloaded.", total);
    }

    @Http("/active")
    public void active(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ModuleException, IOException {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(req, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            final boolean isActivated = moduleManager.isActivated(info.id());
            if (!isActivated) {
                try {
                    moduleManager.active(info.id());
                    total++;
                } catch (ModuleException me) {
                    logger.warn("active module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }// try
            } else {
                total++;
            }
        }// for
        output(resp.getWriter(), "total %s module activated.", total);
    }

    @Http("/frozen")
    public void frozen(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ModuleException, IOException {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(req, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            final boolean isActivated = moduleManager.isActivated(info.id());
            if (isActivated) {
                try {
                    moduleManager.frozen(info.id());
                    total++;
                } catch (ModuleException me) {
                    logger.warn("frozen module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }
            } else {
                total++;
            }

        }
        output(resp.getWriter(), "total %s module frozen.", total);
    }

    @Http("/detail")
    public void detail(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ModuleException, IOException {
        final String uniqueId = req.getParameter("id");
        if (StringUtils.isBlank(uniqueId)) {
            // 如果参数不对，则认为找不到对应的沙箱模块，返回400
            resp.sendError(SC_BAD_REQUEST, "id parameter was required.");
            return;
        }

        final Module module = moduleManager.get(uniqueId);
        if (null == module) {
            resp.sendError(SC_BAD_REQUEST, String.format("module[id=%s] is not existed.", uniqueId));
            return;
        }

        final Information info = module.getClass().getAnnotation(Information.class);
        final boolean isActivated = moduleManager.isActivated(info.id());
        final int cCnt = moduleManager.cCnt(info.id());
        final int mCnt = moduleManager.mCnt(info.id());
        final File jarFile = moduleManager.getJarFile(info.id());
        String sb = "" +
                "      ID : " + info.id() + "\n" +
                " VERSION : " + info.version() + "\n" +
                "  AUTHOR : " + info.author() + "\n" +
                "JAR_FILE : " + jarFile.getPath() + "\n" +
                "   STATE : " + (isActivated ? "ACTIVE" : "FROZEN") + "\n" +
                "    MODE : " + ArrayUtils.toString(info.mode()) + "\n" +
                "   CLASS : " + module.getClass().getName() + "\n" +
                "  LOADER : " + module.getClass().getClassLoader() + "\n" +
                "    cCnt : " + cCnt + "\n" +
                "    mCnt : " + mCnt;

        output(resp.getWriter(), sb);

    }

}
