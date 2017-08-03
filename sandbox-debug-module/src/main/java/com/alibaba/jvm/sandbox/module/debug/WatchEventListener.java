package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.module.debug.util.Express;
import com.alibaba.jvm.sandbox.module.debug.util.GaStringUtils;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * 观察命令事件监听器
 * Created by luanjia@taobao.com on 2017/2/24.
 */
public class WatchEventListener implements EventListener {

    private final Printer printer;
    private final Set<DebugModule.Trigger> triggers;
    private final String watchExpress;
    private final int expand;

    private final ThreadLocal<Stack<Object>> stackRef = new ThreadLocal<Stack<Object>>() {
        @Override
        protected Stack<Object> initialValue() {
            return new Stack<Object>();
        }
    };

    public WatchEventListener(Printer printer, Set<DebugModule.Trigger> triggers, String watchExpress, int expand) {
        this.printer = printer;
        this.triggers = triggers;
        this.watchExpress = watchExpress;
        this.expand = expand;
    }

    @Override
    public void onEvent(final Event event) throws Throwable {

        if (event instanceof BeforeEvent) {
            final BeforeEvent beforeEvent = (BeforeEvent) event;
            final Stack<Object> stack = stackRef.get();
            stack.push(beforeEvent.argumentArray);
            stack.push(beforeEvent.javaMethodName);
            stack.push(beforeEvent.javaClassName);
            stack.push(beforeEvent.target);
            stack.push(beforeEvent.javaClassLoader);
            if (triggers.contains(DebugModule.Trigger.BEFORE)) {
                final Map<String, Object> bind = new HashMap<String, Object>();
                bind.put("class", beforeEvent.javaClassName);
                bind.put("method", beforeEvent.javaMethodName);
                bind.put("params", beforeEvent.argumentArray);
                bind.put("target", beforeEvent.target);
                bind.put("loader", beforeEvent.javaClassLoader);
                final Express express = Express.ExpressFactory.newExpress(bind);
                final Object watchObject = express.get(watchExpress);
                printer.println(GaStringUtils.toString(watchObject, expand));
            }

        } else if (event instanceof ReturnEvent
                || event instanceof ThrowsEvent) {

            final Stack<Object> stack = stackRef.get();
            final ClassLoader classLoader = (ClassLoader) stack.pop();
            final Object target = stack.pop();
            final String javaClassName = (String) stack.pop();
            final String javaMethodName = (String) stack.pop();
            final Object[] argumentArray = (Object[]) stack.pop();

            final Map<String, Object> bind = new HashMap<String, Object>();
            bind.put("loader", classLoader);
            bind.put("target", target);
            bind.put("class", javaClassName);
            bind.put("method", javaMethodName);
            bind.put("params", argumentArray);

            if (event instanceof ReturnEvent
                    && triggers.contains(DebugModule.Trigger.RETURN)) {
                final ReturnEvent returnEvent = (ReturnEvent) event;
                bind.put("return", returnEvent.object);
                final Express express = Express.ExpressFactory.newExpress(bind);
                final Object watchObject = express.get(watchExpress);
                printer.println(GaStringUtils.toString(watchObject, expand));
            } else if (event instanceof ThrowsEvent
                    && triggers.contains(DebugModule.Trigger.THROWS)) {
                final ThrowsEvent throwsEvent = (ThrowsEvent) event;
                bind.put("throw", throwsEvent.throwable);
                final Express express = Express.ExpressFactory.newExpress(bind);
                final Object watchObject = express.get(watchExpress);
                printer.println(GaStringUtils.toString(watchObject, expand));
            } else {
                return;
            }

        }

    }

}
