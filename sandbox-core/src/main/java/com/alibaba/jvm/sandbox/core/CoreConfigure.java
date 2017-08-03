package com.alibaba.jvm.sandbox.core;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.core.util.FeatureCodec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 内核启动配置
 * Created by luanjia@taobao.com on 16/10/2.
 */
public class CoreConfigure {

    private static final String KEY_SANDBOX_HOME = "sandbox_home";
    private static final String KEY_LAUNCH_MODE = "mode";
    private static final String KEY_SERVER_IP = "server.ip";
    private static final String KEY_SERVER_PORT = "server.port";

    private static final String KEY_SYSTEM_MODULE_LIB_PATH = "system_module";
    private static final String KEY_USER_MODULE_LIB_PATH = "user_module";
    private static final String KEY_PROVIDER_LIB_PATH = "provider";
    private static final String KEY_CFG_LIB_PATH = "cfg";
    private static final String VAL_LAUNCH_MODE_AGENT = "agent";
    private static final String VAL_LAUNCH_MODE_ATTACH = "attach";
    private static final String KEY_EVENT_POOL_ENABLE = "event.pool.enable";
    private static final String KEY_EVENT_POOL_KEY_MIN = "event.pool.key.min";
    private static final String KEY_EVENT_POOL_KEY_MAX = "event.pool.key.max";
    private static final String KEY_EVENT_POOL_TAL = "event.pool.total";
    private static final String KEY_UNSAFE_ENABLE = "unsafe.enable";

    // 受保护key数组，在保护key范围之内，如果前端已经传递过参数了，只能认前端，后端无法修改
    private static final String[] PROTECT_KEY_ARRAY = {KEY_SANDBOX_HOME, KEY_LAUNCH_MODE, KEY_SERVER_IP, KEY_SERVER_PORT};

    private static final FeatureCodec codec = new FeatureCodec(';', '=');
    private final Map<String, String> featureMap;

    private CoreConfigure(final String featureString) {
        this.featureMap = codec.toMap(featureString);
    }

    private static volatile CoreConfigure instance;

    public static CoreConfigure toConfigure(final String featureString, final String propertiesFilePath) {
        return instance = mergePropertiesFile(new CoreConfigure(featureString), propertiesFilePath);
    }

    // 从配置文件中合并配置到CoreConfigure中
    private static CoreConfigure mergePropertiesFile(final CoreConfigure cfg, final String propertiesFilePath) {
        cfg.featureMap.putAll(propertiesToStringMap(fetchProperties(propertiesFilePath)));
        return cfg;
    }

    // 从指定配置文件路径中获取配置信息
    private static Properties fetchProperties(final String propertiesFilePath) {
        final Properties properties = new Properties();
        InputStream is = null;
        try {
            is = FileUtils.openInputStream(new File(propertiesFilePath));
            properties.load(is);
        } catch (Throwable cause) {
            // cause.printStackTrace(System.err);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return properties;
    }

    // 配置转map
    private static Map<String, String> propertiesToStringMap(final Properties properties) {
        final Map<String, String> map = new HashMap<String, String>();
        for (String key : properties.stringPropertyNames()) {

            // 过滤掉受保护的key
            if (ArrayUtils.contains(PROTECT_KEY_ARRAY, key)
                    && map.containsKey(key)) {
                continue;
            }

            map.put(key, properties.getProperty(key));
        }
        return map;
    }

    public static CoreConfigure getInstance() {
        return instance;
    }


    /**
     * 获取系统模块加载路径
     *
     * @return 模块加载路径
     */
    public String getSystemModuleLibPath() {
        return featureMap.get(KEY_SYSTEM_MODULE_LIB_PATH);
    }

    /**
     * 获取用户模块加载路径
     *
     * @return 用户模块加载路径
     */
    public String getUserModuleLibPath() {
        return featureMap.get(KEY_USER_MODULE_LIB_PATH);
    }

    /**
     * 获取配置文件加载路径
     *
     * @return 配置文件加载路径
     */
    public String getCfgLibPath() {
        return featureMap.get(KEY_CFG_LIB_PATH);
    }

    @Override
    public String toString() {
        return codec.toString(featureMap);
    }

    /**
     * 是否以Agent模式启动
     *
     * @return true/false
     */
    private boolean isLaunchByAgentMode() {
        return StringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_AGENT);
    }

    /**
     * 是否以Attach模式启动
     *
     * @return true/false
     */
    private boolean isLaunchByAttachMode() {
        return StringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_ATTACH);
    }

    /**
     * 获取沙箱的启动模式<br/>
     * 默认按照ATTACH模式启动
     *
     * @return 沙箱的启动模式
     */
    public Information.Mode getLaunchMode() {
        if (isLaunchByAgentMode()) {
            return Information.Mode.AGENT;
        }
        if (isLaunchByAttachMode()) {
            return Information.Mode.ATTACH;
        }
        return Information.Mode.ATTACH;
    }


    /**
     * 获取事件池最小值
     *
     * @return event.pool.min
     */
    public int getEventPoolKeyMin() {
        return NumberUtils.toInt(featureMap.get(KEY_EVENT_POOL_KEY_MIN), Runtime.getRuntime().availableProcessors() * 64);
    }

    /**
     * 获取事件池最大值
     *
     * @return event.pool.max
     */
    public int getEventPoolKeyMax() {
        return NumberUtils.toInt(featureMap.get(KEY_EVENT_POOL_KEY_MAX), Runtime.getRuntime().availableProcessors() * 128);
    }

    /**
     * 获取事件池总体最大值
     *
     * @return event.pool.total
     */
    public int getEventPoolTotal() {
        return NumberUtils.toInt(featureMap.get(KEY_EVENT_POOL_TAL), getEventPoolKeyMax() * 5/*numbers(Event.Type)==5*/);
    }

    /**
     * 是否启用事件池
     *
     * @return event.pool.enable
     */
    public boolean isEventPoolEnable() {
        return BooleanUtils.toBoolean(featureMap.get(KEY_EVENT_POOL_ENABLE));
    }

    /**
     * 是否启用Unsafe功能
     *
     * @return unsafe.enable
     */
    public boolean isEnableUnsafe() {
        return BooleanUtils.toBoolean(featureMap.get(KEY_UNSAFE_ENABLE));
    }

    /**
     * 获取沙箱安装目录
     *
     * @return 沙箱安装目录
     */
    public String getJvmSandboxHome() {
        return featureMap.get(KEY_SANDBOX_HOME);
    }

    /**
     * 获取服务器绑定IP
     *
     * @return 服务器绑定IP
     */
    public String getServerIp() {
        return StringUtils.isNotBlank(featureMap.get(KEY_SERVER_IP))
                ? featureMap.get(KEY_SERVER_IP)
                : "127.0.0.1";
    }

    /**
     * 获取服务器端口
     *
     * @return 服务器端口
     */
    public int getServerPort() {
        return NumberUtils.toInt(featureMap.get(KEY_SERVER_PORT), 0);
    }

    /**
     * 获取沙箱内部服务提供库目录
     *
     * @return 沙箱内部服务提供库目录
     */
    public String getProviderLibPath() {
        return featureMap.get(KEY_PROVIDER_LIB_PATH);
    }

}
