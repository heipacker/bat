package com.dlmu.bat.annotation;

import java.lang.annotation.*;

/**
 * 注解一个方法, 如果需要对一个方法进行包装
 * <pre>
 *       @DTrace("testTraceValue")
 *       public String testMethod(@DP String name1, String name2, @DP int intValue) throws Exception {
 *           File tmpFile = new File("/home/heipacker/test.txt");
 *           if (!tmpFile.exists()) {
 *               tmpFile.createNewFile();
 *           } else {
 *               System.out.println("test");
 *           }
 *           return "sayHelloTwoArgOneReturnThrow";
 *       }
 * </pre>
 *
 * @author heipacker
 * @date 16-4-2
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface DTrace {

    /**
     * span desc
     *
     * @return desc
     */
    String value() default "";

    /**
     * span类型。
     *
     * @return span类型。
     */
    String type() default "DTRACE";
}
