package com.alibaba.jvm.sandbox.api.util;

/**
 * A protector that do not allow any events, which is useful to enhance the perfomance when initializing as:
 *
 * <code>
 *         SandboxProtector currentProtector = SandboxProtector.SandboxProtectors.getInstance();
 *         SandboxProtector stopAllSandboxProtector = new StopAllSandboxProtector();
 *         SandboxProtector.SandboxProtectors.force2resetInstance(stopAllSandboxProtector);
 *         try {
 *             initialize();
 *         } finally {
 *             SandboxProtector.SandboxProtectors.force2resetInstance(currentProtector);
 *         }
 * </code>
 *
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/6 4:37 下午
 */
public class StopAllSandboxProtector implements SandboxProtector {


    public StopAllSandboxProtector() {
    }

    @Override
    public int enterProtecting() {
        return 0;
    }

    @Override
    public int exitProtecting() {
        return 0;
    }

    @Override
    public <T> T protectProxy(Class<T> protectTargetInterface, final T protectTarget) {
        return protectTarget;
    }

    @Override
    public boolean isInProtecting() {
        return true;
    }

}
