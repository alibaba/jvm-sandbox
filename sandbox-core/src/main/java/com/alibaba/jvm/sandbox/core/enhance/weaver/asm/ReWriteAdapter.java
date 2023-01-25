package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.com.alibaba.jvm.sandbox.spy.Spy;

/**
 * 方法重写
 * ReWriteJavaMethod
 * Created by luanjia@taobao.com on 16/5/20.
 */
public class ReWriteAdapter extends CodeLockAdapter implements Opcodes, AsmTypes, AsmMethods {

    private final Type[] argumentTypeArray;

    /**
     * Creates a new {@link AdviceAdapter}.
     *
     * @param api    the ASM API version implemented by this visitor. Must be one
     *               of {@link Opcodes#ASM4} or {@link Opcodes#ASM7}.
     * @param mv     the method visitor to which this adapter delegates calls.
     * @param access the method's access flags (see {@link Opcodes}).
     * @param name   the method's name.
     * @param desc   the method's descriptor (see {@link Type Type}).
     */
    protected ReWriteAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
        this.argumentTypeArray = Type.getArgumentTypes(desc);
    }

    /**
     * 将NULL压入栈
     */
    final protected void pushNull() {
        push((Type) null);
    }

    /**
     * 是否静态方法
     *
     * @return true:静态方法 / false:非静态方法
     */
    final protected boolean isStaticMethod() {
        return (methodAccess & ACC_STATIC) != 0;
    }

    /**
     * 加载this/null
     */
    final protected void loadThisOrPushNullIfIsStatic() {
        if (isStaticMethod()) {
            pushNull();
        } else {
            loadThis();
        }
    }

    /**
     * 保存参数数组
     */
    final protected void storeArgArray() {
        for (int i = 0; i < argumentTypeArray.length; i++) {
            dup();
            push(i);
            arrayLoad(ASM_TYPE_OBJECT);
            unbox(argumentTypeArray[i]);
            storeArg(i);
        }
    }

    /**
     * 加载返回值
     * @param returnType 返回值类型
     */
    final protected void loadReturn(Type returnType) {
        final int sort = returnType.getSort();
        switch (sort) {
            case Type.VOID: {
                pushNull();
                break;
            }
            case Type.LONG:
            case Type.DOUBLE: {
                dup2();
                box(Type.getReturnType(methodDesc));
                break;
            }
            case Type.ARRAY:
            case Type.OBJECT: {
                dup();
                break;
            }
            case Type.METHOD:
            default: {
                dup();
                box(Type.getReturnType(methodDesc));
                break;
            }
        }
    }

    // 拆箱返回
    // 比如返回类型为double时，Ret#respond类型为Double，此时需要拆箱操作
    private void unboxReturn(Type returnType) {
        /*
         * [respond]
         */
        final int sort = returnType.getSort();
        switch (sort) {
            case Type.VOID: {
                pop();
                /*
                 * []
                 */
                mv.visitInsn(Opcodes.RETURN);
                break;
            }
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT: {
                unbox(returnType);
                /*
                  [unBox respond]
                 */
                returnValue();
                break;
            }
            case Type.FLOAT: {
                unbox(returnType);
                /*
                  [unBox respond]
                 */
                mv.visitInsn(Opcodes.FRETURN);
                break;
            }
            case Type.LONG: {
                unbox(returnType);
                /*
                 * [unBox respond]
                 */
                mv.visitInsn(Opcodes.LRETURN);
                break;
            }
            case Type.DOUBLE: {
                unbox(returnType);
                /*
                 * [unBox respond]
                 */
                mv.visitInsn(Opcodes.DRETURN);
                break;
            }
            case Type.ARRAY:
            case Type.OBJECT:
            case Type.METHOD:
            default: {
                // checkCast(returnType);
                unbox(returnType);
                /*
                 * [unBox respond]
                 */
                mv.visitInsn(ARETURN);
                break;
            }

        }
    }

    // 销毁栈顶原有元素
    private void popRawRespond(Type returnType) {
        final int sort = returnType.getSort();
        switch (sort) {
            case Type.VOID: {
                break;
            }
            case Type.LONG:
            case Type.DOUBLE: {
                dupX2();
                pop();
                pop2();
                break;
            }
            default: {
                swap();
                pop();
                break;
            }
        }
    }

    /**
     * 方法流程控制
     *
     * @param desc            方法ASM描述
     * @param isPopRawRespond 是否需要弹出栈返回值
     *                        在RETURN和THROWS的事件流程控制中，栈顶是有元素的。
     *                        流程控制过程中如遇到需要改变现有流程情况需要销毁原有栈顶元素
     */
    final protected void processControl(String desc, boolean isPopRawRespond) {
        final Label finishLabel = new Label();
        final Label returnLabel = new Label();
        final Label throwsLabel = new Label();
        /*
         * {rawRespond} 表示 isPopRawRespond = true 时才会存在
         *
         * [Ret, {rawRespond}]
         */
        dup();
        /*
         * [Ret, Ret, {rawRespond}]
         */
        visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET.getInternalName(), "state", ASM_TYPE_INT.getDescriptor());
        /*
         * [I, Ret, {rawRespond}]
         */
        dup();
        /*
         * [I,I, Ret, {rawRespond}]
         */
        push(Spy.Ret.RET_STATE_RETURN);
        /*
         * [I,I,I, Ret, {rawRespond}]
         */
        ifICmp(EQ, returnLabel);
        /*
         * [I, Ret, {rawRespond}]
         */
        push(Spy.Ret.RET_STATE_THROWS);
        /*
         * [I, I, Ret, {rawRespond}]
         */
        ifICmp(EQ, throwsLabel);
        /*
         * [Ret, {rawRespond}]
         */
        goTo(finishLabel);
        mark(returnLabel);
        /*
         * [I, Ret, {rawRespond}]
         */
        pop();
        Type type = Type.getReturnType(desc);
        /*
         * [Ret, {rawRespond}]
         * #fix issue #328
         */
        if (isPopRawRespond) {
            popRawRespond(type);
        }
        /*
         * [Ret]
         */
        visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET.getInternalName(), "respond", ASM_TYPE_OBJECT.getDescriptor());
        /*
         *  [spyRespond] ,execute XReturn
         */
        unboxReturn(type);
        /*
         * [spyRespond] Return Exit
         * [spyRespond]
         */
        mark(throwsLabel);
        /*
         * [Ret, {rawRespond}]
         */
        if (isPopRawRespond) {
            popRawRespond(type);
        }
        /*
         * [Ret]
         */
        visitFieldInsn(GETFIELD, ASM_TYPE_SPY_RET.getInternalName(), "respond", ASM_TYPE_OBJECT.getDescriptor());
        /*
         * [Object]
         */
        checkCast(ASM_TYPE_THROWABLE);
        /*
         * [Throwable]
         */
        throwException();
        /*
         * throw [Throwable] Exit
         */
        mark(finishLabel);
        /*
         * [Ret, {raw respond}]
         */
        pop();
        /*
         * [{raw respond}]
         *  None Exit
         */
    }
}
