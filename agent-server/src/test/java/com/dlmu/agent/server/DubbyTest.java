package com.dlmu.agent.server;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by fupan on 16-4-9.
 */
public class DubbyTest {

    @Test
    public void easyDubbyTest() throws IllegalAccessException, InstantiationException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("easyDubbyTest")
                .method(named("toString"))
                .intercept(FixedValue.value("Hello World!"))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object newInstance = dynamicType.newInstance();
        assertThat(newInstance.toString(), is("Hello World!"));
        assertThat(newInstance.getClass().getSimpleName(), is("easyDubbyTest"));
    }

    /**
     * 代理一个类的方法
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void complexDubbyTest() throws IllegalAccessException, InstantiationException {
        Class<? extends java.util.function.Function> dynamicType = new ByteBuddy()
                .subclass(java.util.function.Function.class)
                .method(named("apply"))
                .intercept(MethodDelegation.to(new GreetingInterceptor()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat((String) dynamicType.newInstance().apply("Byte Buddy"), is("Hello from Byte Buddy"));
    }

    /**
     * 测试代理一个类， 用一个固定值 修改返回值
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void fooDubbyTest() throws IllegalAccessException, InstantiationException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        System.out.printf(classLoader.toString());
        Foo dynamicFoo = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class)).intercept(FixedValue.value("One!"))
                .method(named("foo")).intercept(FixedValue.value("Two!"))
                .method(named("foo").and(takesArguments(1))).intercept(FixedValue.value("Three!"))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        System.out.println();
        System.out.println("bar:" + dynamicFoo.bar());
        System.out.println("foo:" + dynamicFoo.foo());
        System.out.println("foo 1:" + dynamicFoo.foo(new Object()));
    }

    @Test
    public void sourceDubbyTest() throws IllegalAccessException, InstantiationException {
        String helloWorld = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .hello("World");
        System.out.println("sourceDubbyTest:" + helloWorld);
    }

    public void agentBuilderTest() {
        new AgentBuilder.Default().type(ElementMatchers.any()).transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                return builder
                        .method(ElementMatchers.any()).intercept(MethodDelegation.to(DelagateLogging.class)
                                .andThen(SuperMethodCall.INSTANCE));
            }
        }).installOn(null);
    }
}
