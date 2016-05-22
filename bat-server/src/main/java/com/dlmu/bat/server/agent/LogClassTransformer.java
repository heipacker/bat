package com.dlmu.bat.server.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

/**
 * Created by fupan on 16-4-2.
 */
public class LogClassTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (classBeingRedefined != null) return null;
        Class<?> loggedClass = null;
        try {
            loggedClass = new ByteBuddy()
                    .subclass(loader.loadClass(className))
                    .method(ElementMatchers.any()).intercept(MethodDelegation.to(DelagateLogging.class).filter(ElementMatchers.named("logging")))
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(ClassLoader.getSystemClassLoader().getResourceAsStream(loggedClass.getName()));
        try {
            int available = bufferedInputStream.available();
            ByteBuffer byteBuffer = ByteBuffer.allocate(available);
            byte[] bytes = new byte[1024];
            int readLen;
            int offset = 0;
            while ((readLen = bufferedInputStream.read(bytes)) > 0) {
                byteBuffer.put(bytes, offset, readLen);
                offset += readLen;
            }
            return byteBuffer.array();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }
}
