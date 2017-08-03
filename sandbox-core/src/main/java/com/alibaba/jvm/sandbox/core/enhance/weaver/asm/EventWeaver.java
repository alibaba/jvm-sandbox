package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.core.enhance.weaver.CodeLock;
import com.alibaba.jvm.sandbox.core.util.BitUtils;
import java.com.alibaba.jvm.sandbox.spy.Spy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.jvm.sandbox.util.SandboxStringUtils.toJavaClassName;
import static com.alibaba.jvm.sandbox.util.SandboxStringUtils.toJavaClassNameArray;
import static org.apache.commons.lang3.ArrayUtils.contains;

/**
 * 用于Call的代码锁
 */
class CallAsmCodeLock extends AsmCodeLock {

    CallAsmCodeLock(AdviceAdapter aa) {
        super(
                aa,
                new int[]{
                        ICONST_2, POP
                },
                new int[]{
                        ICONST_3, POP
                }
        );
    }
}

/**
 * TryCatch块,用于ExceptionsTable重排序
 */
class AsmTryCatchBlock {

    protected final Label start;
    protected final Label end;
    protected final Label handler;
    protected final String type;

    AsmTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

}

/**
 * 方法事件编织者
 * Created by luanjia@taobao.com on 16/7/16.
 */
public class EventWeaver extends ClassVisitor implements Opcodes, AsmTypes, AsmMethods {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int targetClassLoaderObjectID;
    private final int listenerId;
    private final String targetClassInternalName;
    private final String targetJavaClassName;
    private final Type targetClassAsmType;
    private final Filter filter;
    private final AtomicBoolean reWriteMark;

    private final String uniqueCodePrefix;
    private final Set<String> affectMethodUniqueSet;

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
                       final int listenerId,
                       final int targetClassLoaderObjectID,
                       final String targetClassInternalName,
                       final Filter filter,
                       final AtomicBoolean reWriteMark,
                       final String uniqueCodePrefix,
                       final Set<String> affectMethodUniqueSet,
                       final Event.Type[] eventTypeArray) {
        super(api, cv);
        this.targetClassLoaderObjectID = targetClassLoaderObjectID;
        this.listenerId = listenerId;
        this.targetClassInternalName = targetClassInternalName;
        this.targetClassAsmType = Type.getObjectType(targetClassInternalName);
        this.targetJavaClassName = toJavaClassName(targetClassInternalName);
        this.filter = filter;
        this.reWriteMark = reWriteMark;
        this.uniqueCodePrefix = uniqueCodePrefix;
        this.affectMethodUniqueSet = affectMethodUniqueSet;
        this.isLineEnable = contains(eventTypeArray, Event.Type.LINE);
        this.hasCallBefore = contains(eventTypeArray, Event.Type.CALL_BEFORE);
        this.hasCallReturn = contains(eventTypeArray, Event.Type.CALL_RETURN);
        this.hasCallThrows = contains(eventTypeArray, Event.Type.CALL_THROWS);
        this.isCallEnable = hasCallBefore || hasCallReturn || hasCallThrows;
    }

    /**
     * 获取参数类型名称数组
     *
     * @return 参数类型名称数组
     */
    private String[] getParameterTypeArray(String desc) {
        final Type[] typeArray = Type.getArgumentTypes(desc);
        if (ArrayUtils.isEmpty(typeArray)) {
            return null;
        }
        final String[] typeDescArray = new String[typeArray.length];
        for (int index = 0; index < typeArray.length; index++) {
            typeDescArray[index] = typeArray[index].getClassName();
        }
        return typeDescArray;
    }

    /*
     * 是否需要忽略的属性,一些类/方法的前缀说明了他们没有被增强的价值
     *
     */
    private boolean isIgnoreAccess(int access) {
        return BitUtils.isIn(access,
                ACC_ABSTRACT,
                ACC_NATIVE,
                ACC_ANNOTATION,
                ACC_BRIDGE,
                ACC_ENUM,
                ACC_INTERFACE
        );
    }

    /*
     * 是否是负责启动的main函数
     * 这个函数如果被增强了会引起错误,所以千万不能增强,嗯嗯
     */
    private boolean isJavaMain(final int access, String name) {
        return (access & ACC_PUBLIC & ACC_STATIC) == access
                && StringUtils.equals(name, "main");
    }

    /*
     * 是否是静态构造函数,因为静态构造函数只有在Class第一次被加载的时候才会进行执行
     * 所以这里我并不打算增强这个方法,因为实用价值不高
     * 等后续有业务需求一定要增强这个类的时候,我再想办法看看
     */
    private boolean isStaticInit(String name) {
        return StringUtils.equals(name, "<clinit>");
    }

    /**
     * 是否需要忽略
     */
    private boolean isIgnore(MethodVisitor mv, int access, String name, String desc, String[] exceptions) {
        return null == mv
                || isIgnoreAccess(access)
                || isStaticInit(name)
                || isJavaMain(access, name)
                || !filter.doMethodFilter(access, name, toJavaClassNameArray(getParameterTypeArray(desc)), toJavaClassNameArray(exceptions), null);
    }


    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {

        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (isIgnore(mv, access, name, desc, exceptions)) {
            logger.debug("{}.{} was ignored, listener-id={}", targetClassInternalName, name, listenerId);
            return mv;
        }

        logger.debug("{}.{} was matched, prepare to rewrite, listener-id={}", targetClassAsmType, name, listenerId);

        // mark reWrite
        reWriteMark.set(true);

        // 进入方法去重
        affectMethodUniqueSet.add(uniqueCodePrefix + name + desc + signature);

        return new ReWriteMethod(api, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {

            private final Label beginLabel = new Label();
            private final Label endLabel = new Label();

            // 用来标记一个方法是否已经进入
            // JVM中的构造函数非常特殊，super();this();是在构造函数方法体执行之外进行，如果在这个之前进行了任何的流程改变操作
            // 将会被JVM加载类的时候判定校验失败，导致类加载出错
            // 所以这里需要用一个标记为告知后续的代码编织，绕开super()和this()
            private boolean isMethodEnter = false;

            // 代码锁
            private final CodeLock codeLockForTracing = new CallAsmCodeLock(this);

            /**
             * 流程控制
             */
            private void processControl() {
                final Label finishLabel = new Label();
                final Label returnLabel = new Label();
                final Label throwsLabel = new Label();
                dup();
                visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET, "state", ASM_TYPE_INT);
                dup();
                push(Spy.Ret.RET_STATE_RETURN);
                ifICmp(EQ, returnLabel);
                push(Spy.Ret.RET_STATE_THROWS);
                ifICmp(EQ, throwsLabel);
                goTo(finishLabel);
                mark(returnLabel);
                pop();
                visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET, "respond", ASM_TYPE_OBJECT);
                checkCastReturn(Type.getReturnType(desc));
                goTo(finishLabel);
                mark(throwsLabel);
                visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET, "respond", ASM_TYPE_OBJECT);
                checkCast(ASM_TYPE_THROWABLE);
                throwException();
                mark(finishLabel);
                pop();
            }

            /**
             * 加载ClassLoader<br/>
             * 这里分开静态方法中ClassLoader的获取以及普通方法中ClassLoader的获取
             * 主要是性能上的考虑
             */
            private void loadClassLoader() {

                // 这里修改为
                push(targetClassLoaderObjectID);

//                if (this.isStaticMethod()) {
//
////                    // fast enhance
////                    if (GlobalOptions.isEnableFastEnhance) {
////                        visitLdcInsn(Type.getType(String.format("L%s;", internalClassName)));
////                        visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
////                    }
//
//                    // normal enhance
////                    else {
//
//                    // 这里不得不用性能极差的Class.forName()来完成类的获取,因为有可能当前这个静态方法在执行的时候
//                    // 当前类并没有完成实例化,会引起JVM对class文件的合法性校验失败
//                    // 未来我可能会在这一块考虑性能优化,但对于当前而言,功能远远重要于性能,也就不打算折腾这么复杂了
//                    visitLdcInsn(targetJavaClassName);
//                    invokeStatic(ASM_TYPE_CLASS, ASM_METHOD_Class$forName);
//                    invokeVirtual(ASM_TYPE_CLASS, ASM_METHOD_Class$getClassLoader);
////                    }
//
//                } else {
//                    loadThis();
//                    invokeVirtual(ASM_TYPE_OBJECT, ASM_METHOD_Object$getClass);
//                    invokeVirtual(ASM_TYPE_CLASS, ASM_METHOD_Class$getClassLoader);
//                }

            }

            @Override
            protected void onMethodEnter() {

                isMethodEnter = true;
                mark(beginLabel);

                codeLockForTracing.lock(new CodeLock.Block() {
                    @Override
                    public void code() {
                        loadArgArray();
                        dup();
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
                        processControl();
                    }
                });
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
                if (!isThrow(opcode)) {
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            loadReturn(opcode);
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnReturn);
                            processControl();
                        }
                    });
                }
            }

            /**
             * 加载异常
             */
            private void loadThrow() {
                dup();
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                mark(endLabel);
                visitTryCatchBlock(beginLabel, endLabel, mark(), ASM_TYPE_THROWABLE.getInternalName());

                codeLockForTracing.lock(new CodeLock.Block() {
                    @Override
                    public void code() {
                        loadThrow();
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnThrows);
                        processControl();
                    }
                });

                throwException();
                super.visitMaxs(maxStack, maxLocals);
            }

            // 用于tracing的当前行号
            private int tracingCurrentLineNumber = -1;

            @Override
            public void visitLineNumber(final int lineNumber, Label label) {
                if (isMethodEnter && isLineEnable) {
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            push(lineNumber);
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnLine);
                        }
                    });
                }
                super.visitLineNumber(lineNumber, label);
                this.tracingCurrentLineNumber = lineNumber;
            }

            @Override
            public void visitInsn(int opcode) {
                super.visitInsn(opcode);
                codeLockForTracing.code(opcode);
            }

            @Override
            public void visitMethodInsn(final int opcode,
                                        final String owner,
                                        final String name,
                                        final String desc,
                                        final boolean itf) {

                // 如果CALL事件没有启用，则不需要对CALL进行增强
                // 如果正在CALL的方法来自于SANDBOX本身，则不需要进行追踪
                if (!isMethodEnter || !isCallEnable || codeLockForTracing.isLock()) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }

                if (hasCallBefore) {
                    // 方法调用前通知
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            push(tracingCurrentLineNumber);
                            push(toJavaClassName(owner));
                            push(name);
                            push(desc);
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallBefore);
                        }
                    });
                }

                // 如果没有CALL_THROWS事件,其实是可以不用对方法调用进行try...catch
                // 这样可以节省大量的字节码
                if (!hasCallThrows) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallReturn);
                        }
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
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallReturn);
                        }
                    });
                }
                goTo(tracingFinallyLabel);

                // }
                // catch
                // {

                catchException(tracingBeginLabel, tracingEndLabel, ASM_TYPE_THROWABLE);
                codeLockForTracing.lock(new CodeLock.Block() {
                    @Override
                    public void code() {
                        dup();
                        invokeVirtual(ASM_TYPE_OBJECT, ASM_METHOD_Object$getClass);
                        invokeVirtual(ASM_TYPE_CLASS, ASM_METHOD_Class$getName);
                        push(listenerId);
                        invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnCallThrows);
                    }
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
            private final ArrayList<AsmTryCatchBlock> asmTryCatchBlocks = new ArrayList<AsmTryCatchBlock>();

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                asmTryCatchBlocks.add(new AsmTryCatchBlock(start, end, handler, type));
            }

            @Override
            public void visitEnd() {
                for (AsmTryCatchBlock tcb : asmTryCatchBlocks) {
                    super.visitTryCatchBlock(tcb.start, tcb.end, tcb.handler, tcb.type);
                }
                super.visitEnd();
            }

        };
    }
}