package com.dlmu.test.asm;

/**
 * @author heipacker
 * @date 16-5-21.
 */
public class ReturnValueTest {

    public void testReturnValue() {
        String ret = testValue();
        System.out.println(ret.toString());
    }

    public String testValue() {
        return "test";
    }

}
