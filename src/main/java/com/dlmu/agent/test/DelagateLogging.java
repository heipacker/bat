package com.dlmu.agent.test;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;

/**
 * Created by fupan on 16-4-9.
 */
public class DelagateLogging {
/*
    @RuntimeType
    public static Object logging(@SuperCall Callable<?> superMethod, @Origin Method method) throws Exception {
        System.out.println("logging begin.");
        Object ret = superMethod.call();
        System.out.println("logging begin.");
        return ret;
    }*/


    public static void logging(@Origin Method method) {
        System.out.println("logging");
    }
}
