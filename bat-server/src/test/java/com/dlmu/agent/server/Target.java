package com.dlmu.agent.server;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Created by fupan on 16-4-9.
 */
public class Target {

    @RuntimeType
    public static Object intercept(@SuperCall Callable<?> callable, @Origin Method name) throws Exception {
        System.out.println("logging before " + name + ".");
        Object ret = null;
        try {
            ret = callable.call();
        } finally {
            System.out.println("logging after " + name + ".");
        }
        return ret;
    }
}
