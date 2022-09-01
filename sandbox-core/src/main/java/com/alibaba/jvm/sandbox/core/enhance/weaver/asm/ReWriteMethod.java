package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

import java.com.alibaba.jvm.sandbox.spy.Spy;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 方法重写
 * ReWriteJavaMethod
 * Created by luanjia@taobao.com on 16/5/20.
 */
public class ReWriteMethod extends AdviceAdapter implements Opcodes, AsmTypes, AsmMethods {

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
    protected ReWriteMethod(int api, MethodVisitor mv, int access, String name, String desc) {
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

    final protected void checkCastReturn(Type returnType) {
        final int sort = returnType.getSort();
        switch (sort) {
            case Type.VOID: {
                pop();
                mv.visitInsn(Opcodes.RETURN);
                break;
            }
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT: {
                unbox(returnType);
                returnValue();
                break;
            }
            case Type.FLOAT: {
                unbox(returnType);
                mv.visitInsn(Opcodes.FRETURN);
                break;
            }
            case Type.LONG: {
                unbox(returnType);
                mv.visitInsn(Opcodes.LRETURN);
                break;
            }
            case Type.DOUBLE: {
                unbox(returnType);
                mv.visitInsn(Opcodes.DRETURN);
                break;
            }
            case Type.ARRAY:
            case Type.OBJECT:
            case Type.METHOD:
            default: {
                // checkCast(returnType);
                unbox(returnType);
                mv.visitInsn(ARETURN);
                break;
            }

        }
    }

    final protected void visitFieldInsn(int opcode, Type owner, String name, Type type) {
        super.visitFieldInsn(opcode, owner.getInternalName(), name, type.getDescriptor());
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

    final protected void loadReturn(Type returnType){
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


    final protected void popRawRespond(Type returnType) {
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

    final protected void processControl(String desc) {
       processControl(desc,false);
    }

    final protected void processControl(String desc,boolean isPopRawRespond) {
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
        //此时可能的栈状态 [Ret,raw object] | [Ret,[raw long high],[raw long low]],需要处理掉栈底原始需要返回的对象 #fix issue #328
        if(isPopRawRespond){
            popRawRespond(Type.getReturnType(desc));
        }
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
}
