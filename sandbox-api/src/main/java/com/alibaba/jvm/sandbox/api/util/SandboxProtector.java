package com.alibaba.jvm.sandbox.api.util;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Sandbox守护者
 * <p>
 * 用来保护sandbox的操作所产生的事件不被响应
 * </p>
 *
 * @author oldmanpushcart@gamil.com
 */
public interface SandboxProtector {

    /**
     * 进入守护区域
     *
     * @return 守护区域当前引用计数
     */
    int enterProtecting();

    /**
     * 离开守护区域
     *
     * @return 守护区域当前引用计数
     */
    int exitProtecting();

    /**
     * 守护接口定义的所有方法
     *
     * @param protectTargetInterface 保护目标接口类型
     * @param protectTarget          保护目标接口实现
     * @param <T>                    接口类型
     * @return 被保护的目标接口实现
     */
    <T> T protectProxy(final Class<T> protectTargetInterface, final T protectTarget);

    /**
     * 判断当前是否处于守护区域中
     *
     * @return TRUE:在守护区域中；FALSE：非守护区域中
     */
    boolean isInProtecting();

    class SandboxProtectors {

        private static AtomicReference<SandboxProtector> sandboxProtectors = new AtomicReference<SandboxProtector>();

        public static void prepare(SandboxProtector sandboxProtector) {
            sandboxProtectors.weakCompareAndSet(null, sandboxProtector);
        }

        /**
         * Force to reset instance. DO NOT USE THIS METHOD IF NO NECESSARY.
         *
         * @param sandboxProtector
         */
        public static void force2resetInstance(SandboxProtector sandboxProtector) {
            sandboxProtectors.set(sandboxProtector);
        }

        public static SandboxProtector getInstance() {
            return sandboxProtectors.get();
        }

    }

}
