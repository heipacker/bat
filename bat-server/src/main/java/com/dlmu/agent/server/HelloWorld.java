package com.dlmu.agent.server;

import com.dlmu.agent.annotation.DTrace;

/**
 * Created by fupan on 16-4-2.
 */
public class HelloWorld {

    @DTrace
    public void sayHello() {
        System.out.println("sayHello");
    }

    public static void main(String[] args) {
        try {
            System.out.println(HelloWorld.class.getName());
            ClassLoader.getSystemClassLoader().loadClass(HelloWorld.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("hello, world.");
    }
}
