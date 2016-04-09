package com.dlmu.dubby;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.reflect.Method;

/**
 * Created by fupan on 16-4-9.
 */
public class Target {

    public static String hello(@Origin Method name) {
        return "Hello " + name + "!";
    }
}
