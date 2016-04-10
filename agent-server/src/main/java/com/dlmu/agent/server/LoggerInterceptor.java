package com.dlmu.agent.server;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Created by fupan on 16-4-9.
 */
public class LoggerInterceptor {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<?> zuper, @Origin Method method) throws Exception {
        System.out.println("logging before " + method.getName());
        Object ret = null;
        try {
            ret = zuper.call();
        } finally {
            System.out.println("logging after " + method.getName());
        }
        return ret;
    }

    @RuntimeType
    public static Object intercept(@SuperCall Callable<?> zuper, @Origin Constructor method) throws Exception {
        return zuper.call();
    }
}
