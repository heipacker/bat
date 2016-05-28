package com.dlmu.bat.common.transformer;

import com.dlmu.bat.common.tclass.TraceClass;
import com.dlmu.bat.common.tclass.TraceMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;

/**
 * 完成底层字节码替换。
 * <p/>
 * 实现:
 * <pre>
 *     Tracer tracer = new Tracer.Builder().name("test").build();
 *     TraceScope traceScope = tracer.newScope("test");
 *     try {
 *         source(XXX, XXX, XXX);
 *     } catch (RuntimeException e) {
 *         traceScope.addKVAnnotation(Constants.EXCEPTION_KEY, "type:" + e.getClass() + ",message:" + e.getMessage());
 *         traceScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);
 *         throw e;
 *     } finally {
 *         traceScope.close();
 *     }
 * </pre>
 * <p/>
 * <pre>
 *     1.存在注解的方法会进行替换，没有添加注解的方法继续使用原来的字节码。
 *     2.按照方法声明的异常个数追加catch块，没有声明异常，则默认使用RuntimeException作为catch块。
 *     3.追加的新的方法为targetMethodName = "$" + sourceMethodName + "$qtrace$annotation$"，sourceMethodName为原方法名。
 *     4.新方法私有(private)。
 *     5.唯一性参照方法名 + 参数类型 + 返回类型。
 *     6.构造方法<init>和静态构造方法<clinit>忽略替换。
 *     7.interface、abstract方法、navicat方法忽略替换。
 *     8.同级方法替换可以支持private方法、final方法、this调用等cglib无法支持的场景。
 * </pre>
 */
final class TraceClassVisitor extends ClassVisitor implements Opcodes {
    private static final Logger logger = LoggerFactory.getLogger(TraceClassVisitor.class);

    private final TraceClass traceClass;
    private final ProtectionDomain protectionDomain;

    private String className;

    public TraceClassVisitor(ClassVisitor cv, TraceClass traceClass, ProtectionDomain protectionDomain) {
        super(ASM5, cv);
        this.traceClass = traceClass;
        this.protectionDomain = protectionDomain;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        TraceMethod traceMethod = traceClass.getMethod(name, desc, protectionDomain);
        if (traceMethod == null) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        logger.info("instrument method: {}.{}", className, name);
        return new TraceMethodVisitor(access, desc, signature, exceptions, className, traceMethod, cv);
    }
}
