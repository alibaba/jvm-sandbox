package com.alibaba.jvm.sandbox.core.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.alibaba.jvm.sandbox.api.routing.LaunchedRouting;
import com.alibaba.jvm.sandbox.api.routing.RoutingExt;
import com.alibaba.jvm.sandbox.api.routing.RoutingInfo;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * {@link SpecialRoutingHandler}
 * <p>
 *
 * @author zhaoyb1990
 */
public class SpecialRoutingHandler {

    private final static Logger logger = LoggerFactory.getLogger(SpecialRoutingHandler.class);

    /**
     * 类路由配置文件名
     */
    private final static String ROUTING_CONFIG_YAML = "META-INF/class-routing-config.yaml";

    private final static String ROUTING_CONFIG_TYPE_YAML = "yaml";

    private final static String ROUTING_CONFIG_TYPE_SPI = "spi";

    /**
     * 通过moduleJarFile解析出路由配置
     *
     * @param moduleJarFile 模块jar文件
     * @return 类路由规则
     * @throws IOException IO异常 - 理论上不会发生，发生异常后逻辑由上层决定
     */
    public static List<RoutingInfo> resolve(File moduleJarFile) throws IOException {

        final List<RoutingInfo> routingExtList = new ArrayList<RoutingInfo>();

        RoutingYaml routingYaml = getRoutingYamlConfig(moduleJarFile);

        if (routingYaml == null) {
            logger.debug("{} - yaml config not found", ROUTING_CONFIG_YAML);
            return routingExtList;
        }

        logger.info("{} - yaml config read finish, config:{}", ROUTING_CONFIG_YAML, routingYaml);
        if (routingYaml.classRoutingEnable) {
            if (ROUTING_CONFIG_TYPE_YAML.equals(routingYaml.routingConfigType)) {
                routingExtList.addAll(loadWithYaml(routingYaml));
            } else if (ROUTING_CONFIG_TYPE_SPI.equals(routingYaml.routingConfigType)) {
                routingExtList.addAll(loadWithSpi(moduleJarFile));
            }
        }

        return routingExtList;
    }

    static List<RoutingInfo> loadWithYaml(RoutingYaml yaml) {
        final List<RoutingInfo> routingExtList = new ArrayList<RoutingInfo>();
        List<RoutingConfig> routingConfigs = yaml.getRoutingConfigs();
        for (RoutingConfig routingConfig : routingConfigs) {
            RoutingInfo routingInfo = transform(routingConfig);
            if (routingInfo != null) {
                routingExtList.add(routingInfo);
            }
        }
        return routingExtList;
    }

    static RoutingInfo transform(RoutingConfig config) {
        RoutingInfo appRouting = LaunchedRouting.useLaunchingRouting(config.getPatternAsArray());
        if (config.isUsingApp() && appRouting != null) {
            return appRouting;
        }
        if (config.isTargetClassType()) {
            return RoutingInfo.withTargetClass(config.targetName, config.getPatternAsArray());
        }
        if (config.isTargetClassloaderType()) {
            return RoutingInfo.withTargetClassloaderName(config.targetName, config.getPatternAsArray());
        }
        return null;
    }

    static List<RoutingInfo> loadWithSpi(File moduleJarFile) {
        final List<RoutingInfo> routingExtList = new ArrayList<RoutingInfo>();
        ModuleJarClassLoader moduleJarClassLoader = null;
        /*
         * 尝试通过SPI方式读取配置 -- 会有额外的Metaspace开销
         */
        try {
            moduleJarClassLoader = new ModuleJarClassLoader(moduleJarFile);
            ServiceLoader<RoutingExt> rExtServiceLoader = ServiceLoader.load(RoutingExt.class, moduleJarClassLoader);
            for (RoutingExt routingExt : rExtServiceLoader) {
                routingExtList.add(routingExt.getSpecialRouting());
            }
            moduleJarClassLoader.closeIfPossible();
        } catch (Throwable throwable) {
            logger.warn("Loading special routing spi from JAR[file={}] failed.", moduleJarFile, throwable);
            if (null != moduleJarClassLoader) {
                moduleJarClassLoader.closeIfPossible();
            }
        }
        return routingExtList;
    }

    static RoutingYaml getRoutingYamlConfig(File moduleJarFile) throws IOException {
        final JarFile moduleJar = new JarFile(moduleJarFile);
        final Enumeration<JarEntry> enumeration = moduleJar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            if (ROUTING_CONFIG_YAML.equals(jarEntry.getName())) {
                InputStream inputStream = null;
                try {
                    inputStream = moduleJar.getInputStream(jarEntry);
                    Yaml yaml = new Yaml(new Constructor(RoutingYaml.class));
                    return yaml.load(inputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
        return null;
    }

    public static class RoutingYaml {

        private boolean classRoutingEnable;
        private String routingConfigType;
        private List<RoutingConfig> routingConfigs;

        public boolean isClassRoutingEnable() {
            return classRoutingEnable;
        }

        public void setClassRoutingEnable(boolean classRoutingEnable) {
            this.classRoutingEnable = classRoutingEnable;
        }

        public String getRoutingConfigType() {
            return routingConfigType;
        }

        public void setRoutingConfigType(String routingConfigType) {
            this.routingConfigType = routingConfigType;
        }

        public List<RoutingConfig> getRoutingConfigs() {
            return routingConfigs;
        }

        public void setRoutingConfigs(
                List<RoutingConfig> routingConfigs) {
            this.routingConfigs = routingConfigs;
        }

        @Override
        public String toString() {
            return "RoutingYaml{" +
                    "classRoutingEnable=" + classRoutingEnable +
                    ", routingConfigType='" + routingConfigType + '\'' +
                    ", routingConfigs=" + routingConfigs +
                    '}';
        }
    }

    public static class RoutingConfig {

        private final static String TARGET_CLASS_TYPE = "targetClass";

        private final static String TARGET_CLASS_LOADER_TYPE = "targetClassloaderName";

        private boolean usingApp;
        private String type;
        private String targetName;
        private List<String> pattern;

        public boolean isUsingApp() {
            return usingApp;
        }

        public void setUsingApp(boolean usingApp) {
            this.usingApp = usingApp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public List<String> getPattern() {
            return pattern;
        }

        public String[] getPatternAsArray() {
            return pattern == null ? new String[0] : pattern.toArray(new String[0]);
        }

        public void setPattern(List<String> pattern) {
            this.pattern = pattern;
        }

        public boolean isTargetClassType() {
            return TARGET_CLASS_TYPE.equals(type);
        }

        public boolean isTargetClassloaderType() {
            return TARGET_CLASS_LOADER_TYPE.equals(type);
        }

        @Override
        public String toString() {
            return "RoutingConfig{" +
                    "usingApp=" + usingApp +
                    ", type='" + type + '\'' +
                    ", targetName='" + targetName + '\'' +
                    ", pattern=" + pattern +
                    '}';
        }
    }
}
