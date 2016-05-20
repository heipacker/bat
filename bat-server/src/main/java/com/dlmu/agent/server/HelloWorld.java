package com.dlmu.agent.server;

import com.dlmu.bat.annotation.DTrace;

/**
 * Created by fupan on 16-4-2.
 */
public class HelloWorld {

    @DTrace
    public void sayHello() {
        System.out.println("sayHello");
    }
}
