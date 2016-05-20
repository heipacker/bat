package com.dlmu.bat.annotation;

import java.lang.annotation.*;

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
