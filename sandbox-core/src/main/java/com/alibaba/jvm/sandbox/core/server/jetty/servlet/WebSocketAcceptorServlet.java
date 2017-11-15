package com.alibaba.jvm.sandbox.core.server.jetty.servlet;

import com.alibaba.jvm.sandbox.api.http.websocket.TextMessageListener;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketAcceptor;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnectionListener;
import com.alibaba.jvm.sandbox.core.domain.CoreModule;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.ModuleResourceManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 构建WebSocket通讯
 * Created by luanjia@taobao.com on 2017/2/2.
 */
public class WebSocketAcceptorServlet extends WebSocketServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // WebSocket Connection auto release Map
    private final ModuleResourceManager moduleResourceManager;

    private final CoreModuleManager coreModuleManager;

    public WebSocketAcceptorServlet(final CoreModuleManager coreModuleManager,
                                    final ModuleResourceManager moduleResourceManager) {
        this.coreModuleManager = coreModuleManager;
        this.moduleResourceManager = moduleResourceManager;
    }


    /**
     * 构造模块的WebSocket通讯连接
     * <p>对应的模块必须实现了{@link WebSocketAcceptor}接口</p>
     * <p>访问的路径为/sandbox/module/websocket/MODULE_NAME</p>
     *
     * @param req      req
     * @param protocol websocket protocol
     * @return WebSocket
     */
    @Override
    public WebSocket doWebSocketConnect(final HttpServletRequest req,
                                        final String protocol) {

        final String uniqueId = parseUniqueId(req.getPathInfo());
        if (StringUtils.isBlank(uniqueId)) {
            logger.warn("websocket value={} is illegal.", req.getPathInfo());
            return null;
        }

        final CoreModule coreModule = coreModuleManager.get(uniqueId);
        if (null == coreModule) {
            logger.warn("module[id={};] was not existed.", uniqueId);
            return null;
        }

        if (!(coreModule.getModule() instanceof WebSocketAcceptor)) {
            logger.warn("module[id={};class={};] is not implements WebSocketAcceptor.",
                    uniqueId, coreModule.getModule().getClass());
            return null;
        }

        final WebSocketConnectionListener listener =
                ((WebSocketAcceptor) coreModule.getModule()).onAccept(req, protocol);
        logger.info("accept websocket connection, module[id={};class={};], value={};",
                uniqueId, coreModule.getModule().getClass(), req.getPathInfo());

        if (listener instanceof TextMessageListener) {
            return new InnerOnTextMessage(uniqueId, (TextMessageListener) listener);
        } else {
            return new InnerWebSocket(uniqueId, listener);
        }
    }


    /*
     * 从pathInfo中提取module's uniqueId
     */
    private String parseUniqueId(final String pathInfo) {
        final String[] pathSegmentArray = StringUtils.split(pathInfo, "/");
        if (null != pathSegmentArray
                && pathSegmentArray.length >= 1) {
            return pathSegmentArray[0];
        }
        return null;
    }

    private class WebSocketConnectionResource extends ModuleResourceManager.WeakResource<WebSocketConnection> {

        final WebSocketConnection conn;

        WebSocketConnectionResource(WebSocketConnection conn) {
            super(conn);
            this.conn = conn;
        }

        @Override
        public void release() {
            if (null != conn) {
                conn.disconnect();
            }
        }

    }

    private class InnerWebSocket implements WebSocket {

        final String uniqueId;
        final WebSocketConnectionListener listener;

        private WebSocketConnection conn = null;

        InnerWebSocket(String uniqueId, WebSocketConnectionListener listener) {
            this.uniqueId = uniqueId;
            this.listener = listener;
        }

        @Override
        public void onOpen(Connection connection) {
            conn = moduleResourceManager.append(
                    uniqueId,
                    new WebSocketConnectionResource(toWebSocketConnection(connection))
            );
            listener.onOpen(conn);
        }

        WebSocketConnection toWebSocketConnection(final WebSocket.Connection connection) {
            return new WebSocketConnection() {
                @Override
                public void write(String data) throws IOException {
                    connection.sendMessage(data);
                }

                @Override
                public void disconnect() {
                    connection.disconnect();
                }

                @Override
                public boolean isOpen() {
                    return connection.isOpen();
                }

                @Override
                public void setMaxIdleTime(int ms) {
                    connection.setMaxIdleTime(ms);
                }
            };
        }

        @Override
        public void onClose(int closeCode, String message) {
            try {
                listener.onClose(closeCode, message);
            } finally {
                moduleResourceManager.remove(uniqueId, conn);
            }
        }

    }

    private class InnerOnTextMessage extends InnerWebSocket implements OnTextMessage {

        private final TextMessageListener textMessageListener;

        InnerOnTextMessage(String uniqueId, TextMessageListener listener) {
            super(uniqueId, listener);
            this.textMessageListener = listener;
        }

        @Override
        public void onMessage(String data) {
            textMessageListener.onMessage(data);
        }

    }

}
