package com.alibaba.jvm.sandbox.module.junit.probe;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;

/**
 * @author <a href="mailto:renyi.cry@alibaba-inc.com">renyi.cry<a/>
 * @date 2020/11/17 4:06 下午
 */
public class StringListener extends AdviceListener {
    @Override
    protected void before(Advice advice) throws Throwable {

        if (!advice.isProcessTop()) {
            return;
        }

        if (advice.getTarget() == null) {
            return;
        }

        String target = (String)advice.getParameterArray()[0];

        try {
            if ("password".equals(target)) {
                System.out.println("has password keyword");
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }


    }

}
