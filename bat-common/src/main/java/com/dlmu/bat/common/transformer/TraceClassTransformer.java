package com.dlmu.bat.common.transformer;

import com.dlmu.bat.common.tclass.Conf;
import com.dlmu.bat.common.tclass.TraceClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author heipacker
 * @date 2016-5-15
 */
public class TraceClassTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(TraceClassTransformer.class);

    private final Conf configuration;

    public TraceClassTransformer(Conf configuration) {
        this.configuration = configuration;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        TraceClass traceClass = configuration.match(className);
        if (traceClass != null) {
            logger.info("instrument class: {}", className);
            ClassReader traceClassReader = new ClassReader(classfileBuffer);

            int flag = ClassWriter.COMPUTE_MAXS;
            //如果低于1.7版本，还是用compute maxs吧
            short version = traceClassReader.readShort(6);
            if (version >= Opcodes.V1_7) {
                flag = ClassWriter.COMPUTE_FRAMES;
            }

            //自动计算stack frame，如果没有开启，如果class是1.7版本的则会抛出java.lang.VerifyError: Expecting a stackmap frame at branch target 这样的异常
            ClassWriter traceClassWriter = new ClassWriter(flag);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(/*new CheckClassAdapter*/(traceClassWriter), traceClass, protectionDomain);
            traceClassReader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            byte[] toByteArray = traceClassWriter.toByteArray();
            writeClassFileByteToFileSystem(toByteArray);
            return toByteArray;
        }
        return classfileBuffer;
    }

    /**
     * todo remove this
     *
     * @param toByteArray
     */
    private static void writeClassFileByteToFileSystem(byte[] toByteArray) {
        File targetFile = new File("/home/heipacker/test.class");
        if (!targetFile.exists()) {
            try {
                if (!targetFile.createNewFile()) {
                    throw new RuntimeException("create " + targetFile.getAbsolutePath() + " file failed");
                }
            } catch (IOException e) {
                logger.error("create file {} failed", targetFile.getAbsolutePath(), e);
            }
        }
        BufferedOutputStream bufferedOutputStream = null;
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
            bufferedOutputStream.write(toByteArray);
        } catch (IOException e) {
            logger.error("write data to file {} failed", targetFile.getAbsolutePath(), e);
        } finally {
            try {
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
            } catch (IOException e) {
                logger.error("close file {} failed", targetFile.getAbsolutePath(), e);
            }
        }

    }
}
