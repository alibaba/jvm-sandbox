package com.alibaba.jvm.sandbox.core.util;

import com.alibaba.jvm.sandbox.core.enhance.weaver.EventListenerHandlers;

import java.com.alibaba.jvm.sandbox.spy.Spy;

import static com.alibaba.jvm.sandbox.core.util.SandboxReflectUtils.unCaughtGetClassDeclaredJavaMethod;

/**
 * Spy类操作工具类
 *
 * @author luajia@taobao.com
 */
public class SpyUtils {

    private static final Initializer isSpyInit = new Initializer();

    /**
     * 初始化Spy类
     *
     * @throws Throwable 初始化失败
     */
    public synchronized static void init() throws Throwable {

        if (isSpyInit.isInitialized()) {
            return;
        }

        isSpyInit.initProcess(new Initializer.Processor() {
            @Override
            public void process() throws Throwable {
                Spy.init(
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onBefore",
                                int.class,
                                int.class,
                                Class.class,
                                String.class,
                                String.class,
                                String.class,
                                Object.class,
                                Object[].class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onReturn",
                                int.class,
                                Class.class,
                                Object.class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onThrows",
                                int.class,
                                Class.class,
                                Throwable.class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onLine",
                                int.class,
                                int.class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallBefore",
                                int.class,
                                int.class,
                                String.class,
                                String.class,
                                String.class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallReturn",
                                int.class
                        ),
                        unCaughtGetClassDeclaredJavaMethod(EventListenerHandlers.class, "onCallThrows",
                                int.class,
                                String.class
                        )
                );
            }
        });

    }

}
