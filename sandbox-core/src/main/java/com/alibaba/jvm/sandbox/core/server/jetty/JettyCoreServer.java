package com.alibaba.jvm.sandbox.core.server.jetty;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.ModuleLifeCycleEventBus;
import com.alibaba.jvm.sandbox.core.manager.ModuleResourceManager;
import com.alibaba.jvm.sandbox.core.manager.ProviderManager;
import com.alibaba.jvm.sandbox.core.manager.impl.*;
import com.alibaba.jvm.sandbox.core.server.CoreServer;
import com.alibaba.jvm.sandbox.core.server.jetty.servlet.ModuleHttpServlet;
import com.alibaba.jvm.sandbox.core.server.jetty.servlet.WebSocketAcceptorServlet;
import com.alibaba.jvm.sandbox.core.util.Initializer;
import com.alibaba.jvm.sandbox.core.util.SandboxStringUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

import static com.alibaba.jvm.sandbox.core.util.NamespaceConvert.initNamespaceConvert;
import static com.alibaba.jvm.sandbox.core.util.NetworkUtils.isPortInUsing;
import static java.lang.String.format;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * Jetty实现的Http服务器
 *
 * @author luanjia@taobao.com
 */
public class JettyCoreServer implements CoreServer {

    private static volatile CoreServer coreServer;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Initializer initializer = new Initializer(true);

    private Server httpServer;
    private CoreConfigure cfg;
    private ModuleResourceManager moduleResourceManager;
    private CoreModuleManager coreModuleManager;

    /**
     * 单例
     *
     * @return CoreServer单例
     */
    public static CoreServer getInstance() {
        if (null == coreServer) {
            synchronized (CoreServer.class) {
                if (null == coreServer) {
                    coreServer = new JettyCoreServer();
                }
            }
        }
        return coreServer;
    }

    public boolean isBind() {
        return initializer.isInitialized();
    }

    @Override
    public void unbind() throws IOException {
        try {

            initializer.destroyProcess(new Initializer.Processor() {
                @Override
                public void process() throws Throwable {
                    if (null != httpServer) {

                        // stop http server
                        logger.info("{} is stopping", JettyCoreServer.this);
                        httpServer.stop();

                    }
                }
            });

            // destroy http server
            logger.info("{} is destroying", this);
            httpServer.destroy();

        } catch (Throwable cause) {
            logger.warn("{} unBind failed.", this, cause);
            throw new IOException("unBind failed.", cause);
        }
    }

    @Override
    public InetSocketAddress getLocal() throws IOException {
        if (!isBind()
                || null == httpServer) {
            throw new IOException("server was not bind yet.");
        }

        SelectChannelConnector scc = null;
        final Connector[] connectorArray = httpServer.getConnectors();
        if (null != connectorArray) {
            for (final Connector connector : connectorArray) {
                if (connector instanceof SelectChannelConnector) {
                    scc = (SelectChannelConnector) connector;
                    break;
                }//if
            }//for
        }//if

        if (null == scc) {
            throw new IllegalStateException("not found SelectChannelConnector");
        }

        return new InetSocketAddress(
                scc.getHost(),
                scc.getLocalPort()
        );
    }

    /*
     * 初始化Jetty's ContextHandler
     */
    private void initJettyContextHandler(final CoreConfigure cfg) {
        final ServletContextHandler context = new ServletContextHandler(NO_SESSIONS);

        final String contextPath = "/sandbox/" + cfg.getNamespace();
        context.setContextPath(contextPath);
        context.setClassLoader(getClass().getClassLoader());

        // web-socket-servlet
        final String wsPathSpec = "/module/websocket/*";
        logger.info("initializing ws-http-handler. path={}", contextPath + wsPathSpec);
        context.addServlet(new ServletHolder(new WebSocketAcceptorServlet(coreModuleManager, moduleResourceManager)), wsPathSpec);

        // module-http-servlet
        final String pathSpec = "/module/http/*";
        logger.info("initializing http-handler. path={}", contextPath + pathSpec);
        context.addServlet(new ServletHolder(new ModuleHttpServlet(coreModuleManager, moduleResourceManager)), pathSpec);

        httpServer.setHandler(context);
    }

    // 初始化Logback日志配置
    private void initLogback(final CoreConfigure cfg) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final JoranConfigurator configurator = new JoranConfigurator();
        final File configureFile = new File(cfg.getCfgLibPath() + File.separator + "sandbox-logback.xml");
        configurator.setContext(loggerContext);
        loggerContext.reset();
        InputStream is = null;
        try {
            is = new FileInputStream(configureFile);
            initNamespaceConvert(cfg.getNamespace());
            configurator.doConfigure(is);
            logger.info(SandboxStringUtils.getLogo());
            logger.info("initializing logback success. file={};", configureFile);
        } catch (Throwable cause) {
            logger.warn("initialize logback failed. file={};", configureFile, cause);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    // 关闭Logback日志框架
    private void closeLogback() {
        try {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        } catch (Throwable cause) {
            //
        }
    }

    private void initHttpServer(final CoreConfigure cfg) {

        // 如果IP:PORT已经被占用，则无法继续被绑定
        // 这里说明下为什么要这么无聊加个这个判断，让Jetty的Server.bind()抛出异常不是更好么？
        // 比较郁闷的是，如果这个端口的绑定是"SO_REUSEADDR"端口可重用的模式，那么这个server是能正常启动，但无法正常工作的
        // 所以这里必须先主动检查一次端口占用情况，当然了，这里也会存在一定的并发问题，BUT，我认为这种概率事件我可以选择暂时忽略
        if (isPortInUsing(cfg.getServerIp(), cfg.getServerPort())) {
            throw new IllegalStateException(format("address[%s:%s] already in using, server bind failed.",
                    cfg.getServerIp(),
                    cfg.getServerPort())
            );
        }

        httpServer = new Server(new InetSocketAddress(cfg.getServerIp(), cfg.getServerPort()));
        if (httpServer.getThreadPool() instanceof QueuedThreadPool) {
            final QueuedThreadPool qtp = (QueuedThreadPool) httpServer.getThreadPool();
            qtp.setName("sandbox-jetty-qtp-" + qtp.hashCode());
        }
    }

    // 关闭HTTP服务器
    private void closeHttpServer() {
        if (null != httpServer) {
            httpServer.destroy();
        }
    }

    // 初始化各种manager
    private void initManager(final Instrumentation inst,
                             final CoreConfigure cfg) {

        final ClassLoader sandboxClassLoader = getClass().getClassLoader();

        // 初始化事件处理总线
        logger.info("initializing manager : EventListenerHandlers");
        EventListenerHandlers.getSingleton();

        // 初始化模块生命周期事件总线
        logger.info("initializing manager : ModuleLifeCycleEventBus");
        final ModuleLifeCycleEventBus moduleLifeCycleEventBus = new DefaultModuleLifeCycleEventBus();

        // 初始化模块资源管理器
        logger.info("initializing manager : ModuleResourceManager");
        moduleLifeCycleEventBus.append(moduleResourceManager = new DefaultModuleResourceManager());

        // 初始化服务管理器
        logger.info("initializing manager : ProviderManager");
        final ProviderManager providerManager = new DefaultProviderManager(cfg, sandboxClassLoader);

        // 初始化模块管理器
        logger.info("initializing manager : CoreModuleManager");
        coreModuleManager = new DefaultCoreModuleManager(
                cfg, inst,
                sandboxClassLoader, new DefaultLoadedClassDataSource(inst, cfg),
                moduleLifeCycleEventBus,
                providerManager
        );

    }

    @Override
    public synchronized void bind(final CoreConfigure cfg, final Instrumentation inst) throws IOException {
        try {
            initializer.initProcess(new Initializer.Processor() {
                @Override
                public void process() throws Throwable {
                    JettyCoreServer.this.cfg = cfg;
                    initLogback(cfg);
                    logger.info("initializing server. cfg={}", cfg);
                    initManager(inst, cfg);
                    initHttpServer(cfg);
                    initJettyContextHandler(cfg);
                    httpServer.start();
                }
            });

            // 初始化加载所有的模块
            try {
                coreModuleManager.reset();
            } catch (Throwable cause) {
                logger.warn("reset occur error when initializing.", cause);
            }

            final InetSocketAddress local = getLocal();
            logger.info("initialized server. actual bind to {}:{}", local.getHostName(), local.getPort());
        } catch (Throwable cause) {
            logger.warn("initialize server failed.", cause);
            throw new IOException("server bind failed.", cause);
        }

        logger.info("{} bind success.", this);
    }

    @Override
    public void destroy() {
        if (isBind()) {
            try {
                unbind();
            } catch (IOException e) {
                logger.warn("{} unBind failed when destroy.", this, e);
            }
        }

        // STOP HTTP-SERVER
        closeHttpServer();

        // STOP LOGBACK
        closeLogback();
    }

    @Override
    public String toString() {
        return format("server[%s:%s]", cfg.getServerIp(), cfg.getServerPort());
    }
}
