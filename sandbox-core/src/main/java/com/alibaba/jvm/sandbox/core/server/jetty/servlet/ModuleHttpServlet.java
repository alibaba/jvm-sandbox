package com.alibaba.jvm.sandbox.core.server.jetty.servlet;

import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.ModuleResourceManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.alibaba.jvm.sandbox.api.util.GaStringUtils.matching;

/**
 * 用于处理模块的HTTP请求
 * Created by luanjia@taobao.com on 2017/2/7.
 */
public class ModuleHttpServlet extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CoreModuleManager coreModuleManager;
    private final ModuleResourceManager moduleResourceManager;

    public ModuleHttpServlet(final CoreModuleManager coreModuleManager,
                             final ModuleResourceManager moduleResourceManager) {
        this.coreModuleManager = coreModuleManager;
        this.moduleResourceManager = moduleResourceManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp, Http.Method.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp, Http.Method.POST);
    }

    private void doMethod(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final Http.Method httpMethod) throws ServletException, IOException {

        // 获取请求路径
        final String path = req.getPathInfo();

        // 获取模块ID
        final String uniqueId = parseUniqueId(path);
        if (StringUtils.isBlank(uniqueId)) {
            logger.warn("http request value={} was not found.", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 获取模块
        final CoreModule coreModule = coreModuleManager.get(uniqueId);
        if (null == coreModule) {
            logger.warn("module[id={}] was not existed, value={};", uniqueId, path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 匹配对应的方法
        final Method method = matchingModuleMethod(
                path,
                httpMethod,
                uniqueId,
                coreModule.getModule().getClass()
        );
        if (null == method) {
            logger.warn("module[id={};class={};] request method not found, value={};",
                    uniqueId,
                    coreModule.getModule().getClass(),
                    path
            );
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else {
            logger.debug("found method[class={};method={};] in module[id={};class={};]",
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    uniqueId,
                    coreModule.getModule().getClass()
            );
        }

        // 生成方法调用参数
        final Object[] parameterObjectArray = generateParameterObjectArray(method, req, resp);

        final PrintWriter writer = resp.getWriter();

        // 调用方法
        moduleResourceManager.append(uniqueId,
                new ModuleResourceManager.WeakResource<PrintWriter>(writer) {

                    @Override
                    public void release() {
                        IOUtils.closeQuietly(get());
                    }

                });
        final boolean isAccessible = method.isAccessible();
        final ClassLoader oriThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            method.setAccessible(true);
            Thread.currentThread().setContextClassLoader(coreModule.getLoader());
            method.invoke(coreModule.getModule(), parameterObjectArray);
            logger.debug("http request value={} invoke module[id={};] {}#{} success.",
                    path, uniqueId, coreModule.getModule().getClass(), method.getName());
        } catch (IllegalAccessException iae) {
            logger.warn("impossible, http request value={} invoke module[id={};] {}#{} occur access denied.",
                    path, uniqueId, coreModule.getModule().getClass(), method.getName(), iae);
            throw new ServletException(iae);
        } catch (InvocationTargetException ite) {
            logger.warn("http request value={} invoke module[id={};] {}#{} failed.",
                    path, uniqueId, coreModule.getModule().getClass(), method.getName(), ite.getTargetException());
            final Throwable targetCause = ite.getTargetException();
            if (targetCause instanceof ServletException) {
                throw (ServletException) targetCause;
            }
            if (targetCause instanceof IOException) {
                throw (IOException) targetCause;
            }
            throw new ServletException(targetCause);
        } finally {
            Thread.currentThread().setContextClassLoader(oriThreadContextClassLoader);
            method.setAccessible(isAccessible);
            moduleResourceManager.remove(uniqueId, writer);
        }

    }


    /**
     * 提取模块ID
     * 模块ID应该在PATH的第一个位置
     *
     * @param path servlet访问路径
     * @return 路径解析成功则返回模块的ID，如果解析失败则返回null
     */
    private String parseUniqueId(final String path) {
        final String[] pathSegmentArray = StringUtils.split(path, "/");
        return ArrayUtils.getLength(pathSegmentArray) >= 1
                ? pathSegmentArray[0]
                : null;
    }


    /**
     * 匹配模块中复合HTTP请求路径的方法
     * 匹配方法的方式是：HttpMethod和HttpPath全匹配
     *
     * @param path          HTTP请求路径
     * @param httpMethod    HTTP请求方法
     * @param uniqueId      模块ID
     * @param classOfModule 模块类
     * @return 返回匹配上的方法，如果没有找到匹配方法则返回null
     */
    private Method matchingModuleMethod(final String path,
                                        final Http.Method httpMethod,
                                        final String uniqueId,
                                        final Class<?> classOfModule) {

        for (final Method method : MethodUtils.getMethodsListWithAnnotation(classOfModule, Http.class)) {
            final Http httpAnnotation = method.getAnnotation(Http.class);
            if(null == httpAnnotation) {
                continue;
            }
            final String pathPattern = "/"+uniqueId+httpAnnotation.value();
            if (ArrayUtils.contains(httpAnnotation.method(), httpMethod)
                    && matching(path, pathPattern)) {
                return method;
            }
        }

        // 找不到匹配方法，返回null
        return null;
    }


    /**
     * 生成方法请求参数数组
     * 主要用于填充HttpServletRequest和HttpServletResponse
     *
     * @param method 模块Java方法
     * @param req    HttpServletRequest
     * @param resp   HttpServletResponse
     * @return 请求方法参数列表
     */
    private Object[] generateParameterObjectArray(final Method method,
                                                  final HttpServletRequest req,
                                                  final HttpServletResponse resp) {

        final Class<?>[] parameterTypeArray = method.getParameterTypes();
        if (ArrayUtils.isEmpty(parameterTypeArray)) {
            return null;
        }
        final Object[] parameterObjectArray = new Object[parameterTypeArray.length];
        for (int index = 0; index < parameterObjectArray.length; index++) {
            final Class<?> parameterType = parameterTypeArray[index];

            // HttpServletRequest
            if (HttpServletRequest.class.isAssignableFrom(parameterType)) {
                parameterObjectArray[index] = req;
                continue;
            }

            // HttpServletResponse
            if (HttpServletResponse.class.isAssignableFrom(parameterType)) {
                parameterObjectArray[index] = resp;
                continue;
            }

        }

        return parameterObjectArray;
    }

}
