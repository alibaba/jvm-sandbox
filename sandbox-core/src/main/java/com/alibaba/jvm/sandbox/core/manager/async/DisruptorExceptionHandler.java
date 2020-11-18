package com.alibaba.jvm.sandbox.core.manager.async;

import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/6/10 11:46 上午
 */
public class DisruptorExceptionHandler implements ExceptionHandler<Object> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
        logger.error(ex.getMessage(), ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
    }

}
