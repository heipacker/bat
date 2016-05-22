package com.dlmu.test.asm;

import com.dlmu.bat.annotation.DF;
import com.dlmu.bat.annotation.DP;
import com.dlmu.bat.annotation.DTrace;

import java.io.File;

/**
 * Created by fupan on 16-4-2.
 */
public class TestService {

    @DF
    private String testField;

    @DTrace
    public void sayHello() {
        System.out.println("sayHello");
    }

    @DTrace
    public void sayHelloOneArg(String name) {
        System.out.println("sayHello to " + name);
    }

    @DTrace
    public String sayHelloTwoArgOneReturn(String name1, String name2) {
        System.out.println(name1  + ":" + name2);
        return "sayHelloTwoArgOneReturn";
    }

    @DTrace("sayHelloTwoArgOneReturnThrowValue")
    public String sayHelloTwoArgOneReturnThrow(@DP String name1, String name2, @DP int intValue) throws Exception {
        File tmpFile = new File("/home/heipacker/test.txt");
        if (!tmpFile.exists()) {
            tmpFile.createNewFile();
        } else {
            System.out.println("test");
        }
        return "sayHelloTwoArgOneReturnThrow";
    }
}
