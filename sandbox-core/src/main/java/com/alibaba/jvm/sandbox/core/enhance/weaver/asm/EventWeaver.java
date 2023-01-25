package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

import com.alibaba.jvm.sandbox.api.event.Event;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toInternalClassName;
import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toJavaClassName;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * 方法事件编织者
 * Created by luanjia@taobao.com on 16/7/16.
 */
public class EventWeaver extends ClassVisitor implements Opcodes, AsmTypes, AsmMethods {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int targetClassLoaderObjectID;
    private final String namespace;
    private final int listenerId;
    private final String targetJavaClassName;
    private final Set<String> signCodes;
    private final Event.Type[] eventTypeArray;
    private final String nativePrefix;
    private final List<ProxyMethod> proxyNativeAsmMethods = new ArrayList<>();

    // 是否支持LINE_EVENT
    // LINE_EVENT需要对Class做特殊的增强，所以需要在这里做特殊的判断
    private final boolean isLineEnable;

    // 是否支持CALL_BEFORE/CALL_RETURN/CALL_THROWS事件
    // CALL系列事件需要对Class做特殊的增强，所以需要在这里做特殊的判断
    private final boolean hasCallThrows;
    private final boolean hasCallBefore;
    private final boolean hasCallReturn;
    private final boolean isCallEnable;

    public EventWeaver(final int api,
                       final ClassVisitor cv,
                       final String namespace,
                       final int listenerId,
                       final int targetClassLoaderObjectID,
                       final String targetClassInternalName,
                       final Set<String/*BehaviorStructure#getSignCode()*/> signCodes,
                       final Event.Type[] eventTypeArray,
                       final String nativePrefix) {
        super(api, cv);
        this.targetClassLoaderObjectID = targetClassLoaderObjectID;
        this.namespace = namespace;
        this.listenerId = listenerId;
        this.targetJavaClassName = toJavaClassName(targetClassInternalName);
        this.signCodes = signCodes;
        this.eventTypeArray = eventTypeArray;
        this.nativePrefix = nativePrefix;

        this.isLineEnable = contains(eventTypeArray, Event.Type.LINE);
        this.hasCallBefore = contains(eventTypeArray, Event.Type.CALL_BEFORE);
        this.hasCallReturn = contains(eventTypeArray, Event.Type.CALL_RETURN);
        this.hasCallThrows = contains(eventTypeArray, Event.Type.CALL_THROWS);
        this.isCallEnable = hasCallBefore || hasCallReturn || hasCallThrows;
    }

    private boolean isMatchedBehavior(final String signCode) {
        return signCodes.contains(signCode);
    }

    private String getBehaviorSignCode(final String name,
                                       final String desc) {
        final StringBuilder sb = new StringBuilder(256).append(targetJavaClassName).append("#").append(name).append("(");

        final Type[] methodTypes = Type.getMethodType(desc).getArgumentTypes();
        if (methodTypes.length != 0) {
            sb.append(methodTypes[0].getClassName());
            for (int i = 1; i < methodTypes.length; i++) {
                sb.append(",").append(methodTypes[i].getClassName());
            }
        }

        return sb.append(")").toString();
    }

    // 是否native方法
    private boolean isNative(final int access) {
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    /*
     * native 方法插桩策略：
     * 1.原始的native变为非native方法，并增加AOP式方法体
     * 2.在AOP中增加调用wrapper后的native方法
     * 3.增加proxy的native方法
     */
    private MethodVisitor rewriteNativeMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {

        //去掉native
        int newAccess = access & ~ACC_NATIVE;

        final MethodVisitor mv = super.visitMethod(newAccess, name, desc, signature, exceptions);
        return new ReWriteAdapter(api, new JSRInlinerAdapter(mv, newAccess, name, desc, signature, exceptions), newAccess, name, desc) {

            private final Label beginLabel = new Label();
            private final Label endLabel = new Label();
            private final Label startCatchBlock = new Label();
            private final Label endCatchBlock = new Label();
            private int newLocal = -1;

            // 加载ClassLoader
            private void loadClassLoader() {
                push(targetClassLoaderObjectID);
            }

            /**
             * 流程控制
             */

            @Override
            public void visitEnd() {
                if (!name.startsWith(nativePrefix)) {
                    getCodeLock().lock(() -> {
                        mark(beginLabel);
                        loadArgArray();
                        dup();
                        push(namespace);
                        push(listenerId);
                        loadClassLoader();
                        push(targetJavaClassName);
                        push(name);
                        push(desc);
                        loadThisOrPushNullIfIsStatic();
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnBefore);
                        swap();
                        storeArgArray();
                        pop();
                        processControl(desc, false);
                        final String proxyMethodName = nativePrefix + name;
                        final ProxyMethod proxyMethod = new ProxyMethod(access, proxyMethodName, desc);
                        final String owner = toInternalClassName(targetJavaClassName);
                        if (!isStaticMethod()) {
                            loadThis();
                        }
                        loadArgs();
                        if (isStaticMethod()) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, proxyMethod.getName(), proxyMethod.getDescriptor(), false);
                        } else {
                            //wrapper的方法永远都是private
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, proxyMethod.getName(), proxyMethod.getDescriptor(), false);
                        }
                        proxyNativeAsmMethods.add(proxyMethod);
                        loadReturn(Type.getReturnType(desc));
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnReturn);
                        processControl(desc, true);
                        returnValue();
                        mark(endLabel);
                        mv.visitLabel(startCatchBlock);
                        visitTryCatchBlock(beginLabel, endLabel, startCatchBlock, ASM_TYPE_THROWABLE.getInternalName());
                        newLocal = newLocal(ASM_TYPE_THROWABLE);
                        storeLocal(newLocal);
                        loadLocal(newLocal);
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnThrows);
                        processControl(desc, false);
                        loadLocal(newLocal);
                        throwException();
                        mv.visitLabel(endCatchBlock);
                    });
                }
                super.visitLocalVariable("t", ASM_TYPE_THROWABLE.getDescriptor(), null, startCatchBlock, endCatchBlock, newLocal);
                super.visitEnd();
            }
        };
    }

    private MethodVisitor rewriteNormalMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new ReWriteAdapter(api, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {

            private final Label beginLabel = new Label();
            private final Label endLabel = new Label();
            private final Label beginCatchBlock = new Label();
            private final Label endCatchBlock = new Label();
            private int newLocal = -1;

            // 用来标记一个方法是否已经进入
            // JVM中的构造函数非常特殊，super();this();是在构造函数方法体执行之外进行，如果在这个之前进行了任何的流程改变操作
            // 将会被JVM加载类的时候判定校验失败，导致类加载出错
            // 所以这里需要用一个标记为告知后续的代码编织，绕开super()和this()
            private boolean isMethodEnter = false;

            // 加载ClassLoader
            private void loadClassLoader() {
                push(targetClassLoaderObjectID);
            }

            @Override
            protected void onMethodEnter() {

                /*
                 * 触发Before事件并执行流程变更逻辑
                 */
                getCodeLock().lock(() -> {
                    mark(beginLabel);
                    loadArgArray();
                    dup();
                    push(namespace);
                    push(listenerId);
                    loadClassLoader();
                    push(targetJavaClassName);
                    push(name);
                    push(desc);
                    loadThisOrPushNullIfIsStatic();
                    invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnBefore);
                    swap();
                    storeArgArray();
                    pop();
                    processControl(desc, false);
                });

                // 标记方法体已进入
                isMethodEnter = true;

            }

            /**
             * 是否抛出异常返回(通过字节码判断)
             *
             * @param opcode 操作码
             * @return true:以抛异常形式返回 / false:非抛异常形式返回(return)
             */
            private boolean isThrow(int opcode) {
                return opcode == ATHROW;
            }

            /**
             * 加载返回值
             * @param opcode 操作吗
             */
            private void loadReturn(int opcode) {
                switch (opcode) {

                    case RETURN: {
                        pushNull();
                        break;
                    }

                    case ARETURN: {
                        dup();
                        break;
                    }

                    case LRETURN:
                    case DRETURN: {
                        dup2();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                    default: {
                        dup();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                }
            }

            @Override
            protected void onMethodExit(final int opcode) {

                if (!isThrow(opcode) && !getCodeLock().isLock()) {

                    /*
                     * 触发Return事件并执行流程变更逻辑
                     */
                    getCodeLock().lock(() -> {
                        loadReturn(opcode);
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnReturn);
                        processControl(desc, true);
                    });

                }
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                mark(endLabel);
                mv.visitLabel(beginCatchBlock);
                visitTryCatchBlock(beginLabel, endLabel, beginCatchBlock, ASM_TYPE_THROWABLE.getInternalName());

                /*
                 * 触发Throw事件并执行流程变更逻辑
                 */
                getCodeLock().lock(() -> {
                    newLocal = newLocal(ASM_TYPE_THROWABLE);
                    storeLocal(newLocal);
                    loadLocal(newLocal);
                    push(namespace);
                    push(listenerId);
                    invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnThrows);
                    processControl(desc, false);
                    loadLocal(newLocal);
                });

                throwException();
                mv.visitLabel(endCatchBlock);
                super.visitMaxs(maxStack, maxLocals);
            }

            // 用于tracing的当前行号
            private int tracingCurrentLineNumber = -1;

            @Override
            public void visitLineNumber(final int lineNumber, Label label) {
                if (isMethodEnter && isLineEnable) {
                    getCodeLock().lock(() -> {
                        push(lineNumber);
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnLine);
                    });
                }
                super.visitLineNumber(lineNumber, label);
                this.tracingCurrentLineNumber = lineNumber;
            }

            @Override
            public void visitMethodInsn(final int opcode,
                                        final String owner,
                                        final String name,
                                        final String desc,
                                        final boolean itf) {

                // 如果CALL事件没有启用，则不需要对CALL进行增强
                // 如果正在CALL的方法来自于SANDBOX本身，则不需要进行追踪
                if (!isMethodEnter || !isCallEnable || getCodeLock().isLock()) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }

                if (hasCallBefore) {
                    // 方法调用前通知
                    getCodeLock().lock(() -> {
                        push(tracingCurrentLineNumber);
                        push(toJavaClassName(owner));
                        push(name);
                        push(desc);
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallBefore);
                    });
                }

                // 如果没有CALL_THROWS事件,其实是可以不用对方法调用进行try...catch
                // 这样可以节省大量的字节码
                if (!hasCallThrows) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    getCodeLock().lock(() -> {
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallReturn);
                    });
                    return;
                }


                // 这里是需要处理拥有CALL_THROWS事件的场景
                final Label tracingBeginLabel = new Label();
                final Label tracingEndLabel = new Label();
                final Label tracingFinallyLabel = new Label();

                // try
                // {

                mark(tracingBeginLabel);
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                mark(tracingEndLabel);

                if (hasCallReturn) {
                    // 方法调用后通知
                    getCodeLock().lock(() -> {
                        push(namespace);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallReturn);
                    });
                }
                goTo(tracingFinallyLabel);

                // }
                // catch
                // {

                catchException(tracingBeginLabel, tracingEndLabel, ASM_TYPE_THROWABLE);
                getCodeLock().lock(() -> {
                    dup();
                    invokeVirtual(ASM_TYPE_OBJECT, ASM_METHOD_Object$getClass);
                    invokeVirtual(ASM_TYPE_CLASS, ASM_METHOD_Class$getName);
                    push(namespace);
                    push(listenerId);
                    invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallThrows);
                });

                throwException();

                // }
                // finally
                // {
                mark(tracingFinallyLabel);
                // }

            }

            // 用于try-catch的重排序
            // 目的是让call的try...catch能在exceptions tables排在前边
            private final ArrayList<AsmTryCatchBlock> asmTryCatchBlocks = new ArrayList<>();

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                asmTryCatchBlocks.add(new AsmTryCatchBlock(start, end, handler, type));
            }


            @Override
            public void visitEnd() {
                for (AsmTryCatchBlock tcb : asmTryCatchBlocks) {
                    super.visitTryCatchBlock(tcb.start, tcb.end, tcb.handler, tcb.type);
                }
                super.visitLocalVariable("t", ASM_TYPE_THROWABLE.getDescriptor(), null, beginCatchBlock, endCatchBlock, newLocal);
                super.visitEnd();
            }

        };
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {

        final String signCode = getBehaviorSignCode(name, desc);
        if (!isMatchedBehavior(signCode)) {
            final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            logger.debug("non-rewrite method {} for listener[id={}];",
                    signCode,
                    listenerId
            );
            return mv;
        }

        logger.info("rewrite method {} for listener[id={}];event={};",
                signCode,
                listenerId,
                join(eventTypeArray, ",")
        );

        if (isNative(access)) {
            return rewriteNativeMethod(access, name, desc, signature, exceptions);
        } else {
            return rewriteNormalMethod(access, name, desc, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {

        // 代理的native方法追加到类中
        proxyNativeAsmMethods.forEach(method -> {
            final boolean isStatic = (ACC_STATIC & method.access) != 0;
            final int access = ACC_PRIVATE | ACC_NATIVE | ACC_FINAL | (isStatic ? ACC_STATIC : 0);
            cv.visitMethod(access, method.getName(), method.getDescriptor(), null, null)
                    .visitEnd();
        });

        super.visitEnd();
    }

    /**
     * TryCatch块,用于ExceptionsTable重排序
     */
    private static class AsmTryCatchBlock {

        final Label start;
        final Label end;
        final Label handler;
        final String type;

        AsmTryCatchBlock(Label start, Label end, Label handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }

    }

    /**
     * @author zhuangpeng
     * @since 2020/9/13
     */
    private static class ProxyMethod extends org.objectweb.asm.commons.Method {

        public final int access;

        public ProxyMethod(int access , String name, String descriptor) {
            super(name, descriptor);
            this.access = access;
        }
    }

}