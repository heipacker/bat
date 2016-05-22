package com.dlmu.bat.agent.test;

/**
 * Created by heipacker on 16-5-16.
 */
public class TestByteCode {

    private static final String constantStr = "testet";

    public static void test() {
        TestByteCode testByteCode = new TestByteCode();
        Object i = testByteCode.test1();
//        String toString = i.toString();
        Object o = testByteCode.test2(constantStr, i);

        System.out.println(o.toString());
    }

    private Object test1() {
        return new Object();
    }

    private void testFinally() {
        try {
            test1();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println(1);
        }
    }

    private Object test2(String str, Object obj) {
        System.out.println(obj);
        System.out.println(str);
        return new Object();
    }
}
