package com.alibaba.jvm.sandbox.qatest.core.enhance.transformer;

import com.alibaba.jvm.sandbox.core.enhance.weaver.asm.AsmMethods;
import com.alibaba.jvm.sandbox.core.enhance.weaver.asm.AsmTypes;
import com.alibaba.jvm.sandbox.core.util.AsmUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static com.alibaba.jvm.sandbox.core.util.SandboxStringUtils.toJavaClassName;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ASM7;

/**
 * 测试第三方增强冲突情况
 *
 * @author zhuangpeng
 * @since 2020/6/11
 */
public class TestThirdEnhance{

    private static final boolean isDumpClass = false;

    final Set<String/*BehaviorStructure#getSignCode()*/> signCodes;

    public TestThirdEnhance(Set<String> signCodes) {this.signCodes = signCodes;}

    public byte[] transform(ClassLoader loader, byte[] classfileBuffer){
        // 返回增强后字节码
        final ClassReader cr = new ClassReader(classfileBuffer);
        final ClassWriter cw = createClassWriter(loader, cr);
        cr.accept(new ThirdClassVisitor(ASM7,cw,signCodes,cr.getClassName()),
            EXPAND_FRAMES
        );
        return dumpClassIfNecessary(cr.getClassName(), cw.toByteArray());
    }

    private byte[] dumpClassIfNecessary(String className, byte[] data) {
        if (!isDumpClass) {
            return data;
        }
        final File dumpClassFile = new File("./sandbox-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs()
            && !classPath.exists()) {
            return data;
        }

        // 将类字节码写入文件
        try {
            writeByteArrayToFile(dumpClassFile, data);
        } catch (IOException e) {
        }

        return data;
    }

    private ClassWriter createClassWriter(final ClassLoader targetClassLoader,
        final ClassReader cr) {
        return new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {

            /*
             * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
             * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
             * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
             * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
             *
             * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
             * 规避了原有代码采用Object.class.getClassLoader()的方式
             */
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return AsmUtils.getCommonSuperClass(type1, type2, targetClassLoader);
            }

        };
    }


   static Method method;

    {
        try {
            method = Method.getMethod(Math.class.getDeclaredMethod("random"));
        } catch (NoSuchMethodException e) {

        }
    }

    public static class ThirdClassVisitor extends ClassVisitor {

        private final Set<String> signCodes;
        private final String targetJavaClassName;

        public ThirdClassVisitor(int api,ClassVisitor cv, Set<String> signCodes,String targetClassInternalName) {
            super(api,cv);
            this.signCodes=signCodes;
            this.targetJavaClassName = toJavaClassName(targetClassInternalName);
        }

        private String getBehaviorSignCode(final String name,
            final String desc) {
            final StringBuilder sb = new StringBuilder(256).append(targetJavaClassName).append("#").append(name).append("(");

            final org.objectweb.asm.Type[] methodTypes = org.objectweb.asm.Type.getMethodType(desc).getArgumentTypes();
            if (methodTypes.length != 0) {
                sb.append(methodTypes[0].getClassName());
                for (int i = 1; i < methodTypes.length; i++) {
                    sb.append(",").append(methodTypes[i].getClassName());
                }
            }

            return sb.append(")").toString();
        }

        private boolean isMatchedBehavior(final String signCode) {
            return signCodes.contains(signCode);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            final String signCode = getBehaviorSignCode(name, desc);
            if (!isMatchedBehavior(signCode)) {
                return mv;
            }


            return new ThirdReWriteMethod(api, mv) {
                @Override
                public void visitInsn(final int opcode) {
                    switch (opcode) {
                        case RETURN:
                        case IRETURN:
                        case FRETURN:
                        case ARETURN:
                        case LRETURN:
                        case DRETURN:
                            onExit();
                            break;
                        default:
                            break;
                    }
                    super.visitInsn(opcode);
                }

                private void onExit() {
                    Label startTryBlock = new Label();
                    Label endTryBlock = new Label();
                    Label startCatchBlock = new Label();
                    Label endCatchBlock = new Label();
                    mv.visitLabel(startTryBlock);
                    //静态调用
                    Type type = Type.getType(Math.class);
                    String owner = type.getInternalName();
                    mv.visitMethodInsn(INVOKESTATIC, owner, method.getName(), method.getDescriptor(), false);
                    mv.visitInsn(POP2);
                    mv.visitLabel(endTryBlock);
                    mv.visitJumpInsn(GOTO,endCatchBlock);
                    mv.visitLabel(startCatchBlock);
                    mv.visitInsn(POP);
                    mv.visitLabel(endCatchBlock);
                    mv.visitTryCatchBlock(startTryBlock,endTryBlock,startCatchBlock,ASM_TYPE_THROWABLE.getInternalName());
                }
            };
        }
    }

    public static class ThirdReWriteMethod extends MethodVisitor  implements Opcodes, AsmTypes, AsmMethods {
        protected ThirdReWriteMethod(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }
    }
}
