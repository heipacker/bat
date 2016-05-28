package com.dlmu.bat.server.agent;

import com.dlmu.bat.annotation.BatTrace;
import com.dlmu.bat.server.agent.listener.AgentListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * Created by fupan on 16-4-9.
 */
public class ClassTransformerSupport {

    public static void addCode(Instrumentation instrumentation) {
        Class[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        System.out.println("all load classes length:" + allLoadedClasses.length);
        System.out.println("object size:" + instrumentation.getObjectSize(new Object()));
        /*instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classBeingRedefined != null) return null;
                System.out.println("class transform " + className);
                LogClassTransformer logClassTransformer = new LogClassTransformer();
                return logClassTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        });*/
       /* ClassFileTransformer classFileTransformer = new AgentBuilder.Default().type(ElementMatchers.any()).transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                return builder
                        .method(ElementMatchers.any())
                        .intercept(MethodDelegation.to(DelagateLogging.class)
                                .andThen(SuperMethodCall.INSTANCE
                                        .andThen(MethodDelegation.to(DelagateLogging.class))));
            }
        }).installOn(instrumentation);*/
        ClassFileTransformer classFileTransformer1 = new AgentBuilder.Default().type(nameStartsWith("com.dlmu")).transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                System.out.println("getCanonicalName:" + typeDescription.getCanonicalName());
//                System.out.println("fullClassName:" + typeDescription.getPackage().getName() + "." + typeDescription.getSimpleName());
                return builder.method(isAnnotatedWith(BatTrace.class)).
                        intercept((MethodDelegation.to(LoggerInterceptor.class)));
            }
        }).with(new AgentListener()).installOn(instrumentation);
    }

}
