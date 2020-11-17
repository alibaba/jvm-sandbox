package com.alibaba.jvm.sandbox.module.junit.mock;

import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.core.manager.ProviderManager;

import java.io.File;

public class DummyProviderManager implements ProviderManager {
    @Override
    public void loading(File moduleJarFile) {

    }

    @Override
    public void loading(String uniqueId, Class moduleClass, Module module, File moduleJarFile, ClassLoader moduleClassLoader) throws Throwable {

    }
}
