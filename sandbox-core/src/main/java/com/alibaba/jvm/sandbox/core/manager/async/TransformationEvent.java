package com.alibaba.jvm.sandbox.core.manager.async;

import com.alibaba.jvm.sandbox.core.manager.impl.DefaultModuleEventWatcher;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/6/10 11:34 上午
 */
public class TransformationEvent extends TransformationQuintuple {

    private DefaultModuleEventWatcher defaultModuleEventWatcher;

    public DefaultModuleEventWatcher getDefaultModuleEventWatcher() {
        return defaultModuleEventWatcher;
    }

    public void setDefaultModuleEventWatcher(DefaultModuleEventWatcher defaultModuleEventWatcher) {
        this.defaultModuleEventWatcher = defaultModuleEventWatcher;
    }

}
