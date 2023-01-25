package com.alibaba.jvm.sandbox.core.enhance.weaver.asm;

import com.alibaba.jvm.sandbox.core.enhance.weaver.CodeLock;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 代码锁适配器
 */
class CodeLockAdapter extends AdviceAdapter {

    private final CodeLock codeLock = new InnerAsmCodeLock(this);

    /**
     * Constructs a new {@link AdviceAdapter}.
     *
     * @param api           the ASM API version implemented by this visitor.
     * @param methodVisitor the method visitor to which this adapter delegates calls.
     * @param access        the method's access flags
     * @param name          the method's name.
     * @param descriptor    the method's descriptor
     */
    protected CodeLockAdapter(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
    }

    /**
     * 获取代码锁
     * @return 代码锁
     */
    protected CodeLock getCodeLock() {
        return codeLock;
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        codeLock.code(opcode);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode,operand);
        codeLock.code(opcode);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode,var);
        codeLock.code(opcode);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode,type);
        codeLock.code(opcode);
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        super.visitFieldInsn(opcode,owner,name,descriptor);
        codeLock.code(opcode);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode,label);
        codeLock.code(opcode);
    }

    public void visitLdcInsn(final Object value) {
        super.visitLdcInsn(value);
        codeLock.code(Opcodes.LDC);
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(var,increment);
        codeLock.code(Opcodes.IINC);
    }


    /**
     * 内部代码锁
     */
    private static class InnerAsmCodeLock extends AsmCodeLock {

        InnerAsmCodeLock(AdviceAdapter aa) {
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
}
