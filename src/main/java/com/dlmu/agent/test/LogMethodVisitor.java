package com.dlmu.agent.test;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by fupan on 16-4-2.
 */
public class LogMethodVisitor extends MethodVisitor implements Opcodes {

    public LogMethodVisitor(MethodVisitor methodVisitor) {
        super(ASM5, methodVisitor);
    }

    @Override
    public void visitCode() {

        super.visitCode();
    }
}
