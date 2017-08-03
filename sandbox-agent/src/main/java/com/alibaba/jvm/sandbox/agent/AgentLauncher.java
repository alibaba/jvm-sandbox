package com.alibaba.jvm.sandbox.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.jar.JarFile;

/**
 * SandboxAgent启动器
 * Created by luanjia@taobao.com on 16/7/30.
 */
public class AgentLauncher {

    private static String substringBeforeLast(String str, String separator) {
        return str.substring(0, str.lastIndexOf(separator));
    }

    // sandbox主目录
    private static final String SANDBOX_HOME
            = substringBeforeLast(AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile(), File.separator)
            + File.separator + "..";

    // sandbox配置文件目录
    private static final String SANDBOX_CFG_PATH
            = SANDBOX_HOME + File.separatorChar + "cfg";

    // 模块目录
    private static final String SANDBOX_MODULE_PATH
            = SANDBOX_HOME + File.separatorChar + "module";

    private static final String SANDBOX_USER_MODULE_PATH
            = System.getProperties().getProperty("user.home")
            + File.separator + ".sandbox-module";

    // sandbox核心工程文件
    private static final String SANDBOX_CORE_JAR_PATH
            = SANDBOX_HOME + File.separatorChar + "lib" + File.separator + "sandbox-core.jar";

    // sandbox-spy工程文件
    private static final String SANDBOX_SPY_JAR_PATH
            = SANDBOX_HOME + File.separatorChar + "lib" + File.separator + "sandbox-spy.jar";

    private static final String SANDBOX_PROPERTIES_PATH
            = SANDBOX_CFG_PATH + File.separator + "sandbox.properties";

    // sandbox-provider库目录
    private static final String SANDBOX_PROVIDER_LIB_PATH
            = SANDBOX_HOME + File.separatorChar + "provider";

    // 启动模式: agent方式加载
    private static final String LAUNCH_MODE_AGENT = "agent";

    // 启动模式: attach方式加载
    private static final String LAUNCH_MODE_ATTACH = "attach";

    // agentmain上来的结果输出到文件${HOME}/.sandbox.token
    private static final String RESULT_FILE_PATH = System.getProperties().getProperty("user.home")
            + File.separator + ".sandbox.token";

    private static final String CLASS_OF_CORE_CONFIGURE = "com.alibaba.jvm.sandbox.core.CoreConfigure";
    private static final String CLASS_OF_JETTY_CORE_SERVER = "com.alibaba.jvm.sandbox.core.server.jetty.JettyCoreServer";

    // 全局持有ClassLoader用于隔离sandbox实现
    private static volatile ClassLoader sandboxClassLoader;


    private static boolean isBlankString(final String string) {
        return null != string
                && string.length() > 0
                && !string.matches("^\\s*$");
    }

    private static String getDefaultString(final String string, final String defaultString) {
        return isBlankString(string)
                ? defaultString
                : string;
    }

    /**
     * 启动加载
     *
     * @param propertiesFilePath 配置文件路径
     * @param inst               inst
     */
    public static void premain(String propertiesFilePath, Instrumentation inst) {
        main(
                SANDBOX_CORE_JAR_PATH,
                String.format(";cfg=%s;system_module=%s;mode=%s;sandbox_home=%s;user_module=%s;provider=%s;",
                        SANDBOX_CFG_PATH, SANDBOX_MODULE_PATH, LAUNCH_MODE_AGENT, SANDBOX_HOME, SANDBOX_USER_MODULE_PATH, SANDBOX_PROVIDER_LIB_PATH),
                getDefaultString(propertiesFilePath, SANDBOX_PROPERTIES_PATH),
                inst
        );
    }

    /**
     * 动态加载
     *
     * @param cfg  config of this attach
     * @param inst inst
     */
    public static void agentmain(String cfg, Instrumentation inst) {


        final String[] cfgSegmentArray = cfg.split(";");

        // token
        final String token = cfgSegmentArray.length >= 1
                ? cfgSegmentArray[0]
                : "";

        // server ip
        final String ip = cfgSegmentArray.length >= 2
                ? cfgSegmentArray[1]
                : "127.0.0.1";

        // server port
        final String port = cfgSegmentArray.length >= 3
                ? cfgSegmentArray[2]
                : "0";

        if (token.matches("^\\s*$")) {
            throw new IllegalArgumentException("sandbox attach token was blank.");
        }

        final InetSocketAddress local = main(
                SANDBOX_CORE_JAR_PATH,
                String.format(";cfg=%s;system_module=%s;mode=%s;sandbox_home=%s;user_module=%s;provider=%s;server.ip=%s;server.port=%s;",
                        SANDBOX_CFG_PATH, SANDBOX_MODULE_PATH, LAUNCH_MODE_ATTACH, SANDBOX_HOME, SANDBOX_USER_MODULE_PATH, SANDBOX_PROVIDER_LIB_PATH, ip, port),
                SANDBOX_PROPERTIES_PATH,
                inst
        );
        writeTokenResult(token, local);
    }

    private static synchronized void writeTokenResult(final String token, final InetSocketAddress local) {
        final File file = new File(RESULT_FILE_PATH);

        if (file.exists()
                && (!file.isFile()
                || !file.canWrite())) {
            throw new RuntimeException("write to result file : " + file + " failed.");
        } else {
            FileWriter fw = null;
            try {
                fw = new FileWriter(file, true);
                fw.append(String.format("%s;%s;%s;\n", token, local.getHostName(), local.getPort()));
                fw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (null != fw) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }


    private static ClassLoader loadOrDefineClassLoader(String coreJar) throws Throwable {

        final ClassLoader classLoader;

        // 如果已经被启动则返回之前启动的ClassLoader
        if (null != sandboxClassLoader) {
            classLoader = sandboxClassLoader;
        }

        // 如果未启动则重新加载
        else {
            classLoader = new SandboxClassLoader(coreJar);
        }

        return sandboxClassLoader = classLoader;
    }

    private static synchronized InetSocketAddress main(final String coreJarPath,
                                                       final String coreFeatureString,
                                                       final String propertiesFilePath,
                                                       final Instrumentation inst) {

        try {

            // 将Spy注入到BootstrapClassLoader
            inst.appendToBootstrapClassLoaderSearch(new JarFile(new File(SANDBOX_SPY_JAR_PATH)));

            // 构造自定义的类加载器，尽量减少Sandbox对现有工程的侵蚀
            final ClassLoader agentLoader = loadOrDefineClassLoader(coreJarPath);

            // CoreConfigure类定义
            final Class<?> classOfConfigure = agentLoader.loadClass(CLASS_OF_CORE_CONFIGURE);

            // 反序列化成CoreConfigure类实例
            final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", String.class, String.class)
                    .invoke(null, coreFeatureString, propertiesFilePath);

            // JtServer类定义
            final Class<?> classOfJtServer = agentLoader.loadClass(CLASS_OF_JETTY_CORE_SERVER);

            // 获取JtServer单例
            final Object objectOfJtServer = classOfJtServer
                    .getMethod("getInstance")
                    .invoke(null);

            // gaServer.isBind()
            final boolean isBind = (Boolean) classOfJtServer.getMethod("isBind").invoke(objectOfJtServer);


            // 如果未绑定,则需要绑定一个地址
            if (!isBind) {
                try {
                    classOfJtServer
                            .getMethod("bind", classOfConfigure, Instrumentation.class)
                            .invoke(objectOfJtServer, objectOfCoreConfigure, inst);
                } catch (Throwable t) {
                    classOfJtServer.getMethod("destroy").invoke(objectOfJtServer);
                    throw t;
                }

            }

            // 返回服务器绑定的地址
            return (InetSocketAddress) classOfJtServer
                    .getMethod("getLocal")
                    .invoke(objectOfJtServer);


        } catch (Throwable cause) {
            throw new RuntimeException("sandbox attach failed.", cause);
        }

    }

}
