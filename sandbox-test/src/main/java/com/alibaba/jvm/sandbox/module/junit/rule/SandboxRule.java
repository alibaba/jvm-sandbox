package com.alibaba.jvm.sandbox.module.junit.rule;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleException;
import com.alibaba.jvm.sandbox.core.CoreConfigure;
import com.alibaba.jvm.sandbox.core.CoreModule;
import com.alibaba.jvm.sandbox.core.manager.CoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.manager.CoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreModuleManager;
import com.alibaba.jvm.sandbox.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.alibaba.jvm.sandbox.core.util.FeatureCodec;
import com.alibaba.jvm.sandbox.module.junit.annotations.PrepareForTest;
import com.alibaba.jvm.sandbox.module.junit.mock.DummyProviderManager;
import com.alibaba.jvm.sandbox.module.junit.mock.MockLoadedClassesOnlyInstrumentation;
import com.alibaba.jvm.sandbox.module.junit.support.JvmHelper;
import com.alibaba.jvm.sandbox.module.junit.support.SandboxModuleJarBuilder;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.alibaba.jvm.sandbox.api.ModuleException.ErrorCode.MODULE_ACTIVE_ERROR;
import static java.io.File.createTempFile;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2019-05-13 18:31
 */
public class SandboxRule implements TestRule {

    private static final MockLoadedClassesOnlyInstrumentation mockInstrumentation
            = new MockLoadedClassesOnlyInstrumentation();

    private String id;

    private String version;

    private boolean unsafeEnable;

    private File tempJarFile;

    @Override
    public Statement apply(final Statement statement, final Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                PrepareForTest prepareForTest = description.getAnnotation(PrepareForTest.class);

                if (prepareForTest != null) {

                    JvmHelper.createJvm(prepareForTest.namespace());

                    CoreLoadedClassDataSource coreLoadedClassDataSource;

                    if (prepareForTest.fastMode()) {

                        unsafeEnable = prepareForTest.isEnableUnsafe();

                        coreLoadedClassDataSource = new DefaultCoreLoadedClassDataSource(mockInstrumentation, unsafeEnable);

                        Class[] classes = prepareForTest.spyClasses();
                        for (Class aClass : classes) {
                            mockInstrumentation.regLoadedClass(aClass);
                        }

                    } else {

                        unsafeEnable = true;

                        coreLoadedClassDataSource = new DefaultCoreLoadedClassDataSource(ByteBuddyAgent.install(), unsafeEnable);

                    }

                    tempJarFile = createTempFile("test-", ".jar");

                    final CoreModuleManager coreModuleManager = buildingCoreModuleManager(
                            coreLoadedClassDataSource,
                            buildingModuleJarFileWithModuleClass(
                                    tempJarFile,
                                    prepareForTest.testModule()
                            )
                    );

                    Information information = (Information) prepareForTest.testModule().getAnnotation(Information.class);

                    version = information.version();

                    final CoreModule secCheckModule = coreModuleManager
                            .getThrowsExceptionIfNull(id = information.id());

                    try {
                        coreModuleManager.active(secCheckModule);
                    } catch (ModuleException me) {
                        Assert.assertEquals(MODULE_ACTIVE_ERROR, me.getErrorCode());
                    }

                }

                statement.evaluate();
            }
        };

    }


    private CoreModuleManager buildingCoreModuleManager(final CoreLoadedClassDataSource coreLoadedClassDataSource,
                                                        final File... moduleJarFiles) throws ModuleException {
        // should be a better
        return new DefaultCoreModuleManager(
                buildingCoreConfigureWithUserModuleLib(moduleJarFiles),
                mockInstrumentation,
                coreLoadedClassDataSource,
                new DummyProviderManager()
        ).reset();
    }

    private File buildingModuleJarFileWithModuleClass(final File targetModuleJarFile,
                                                      final Class<? extends Module>... classOfModules) throws IOException {

        // 沙箱模块构造器，构造一个临时的jar file
        final SandboxModuleJarBuilder sandboxModuleJarBuilder = SandboxModuleJarBuilder.building(targetModuleJarFile);

        // 在这个jar file里加入各个class
        for (final Class<? extends Module> classOfModule : classOfModules) {
            sandboxModuleJarBuilder.putModuleClass(classOfModule);
        }

        return sandboxModuleJarBuilder.build();
    }


    private CoreConfigure buildingCoreConfigureWithUserModuleLib(final File... moduleJarFileArray) {

        final Set<String> moduleJarFilePathSet = new LinkedHashSet<String>();
        for (final File moduleJarFile : moduleJarFileArray) {
            moduleJarFilePathSet.add(moduleJarFile.getPath());
        }

        final Map<String, String> featureMap = new HashMap<String, String>();
        featureMap.put("user_module", StringUtils.join(moduleJarFilePathSet, ";"));
        featureMap.put("system_module", System.getProperty("user.home"));
        featureMap.put("unsafe.enable", String.valueOf(unsafeEnable));

        return CoreConfigure.toConfigure(new FeatureCodec(';', '=').toString(featureMap), null);
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public File getTempJarFile() {
        return tempJarFile;
    }
}
