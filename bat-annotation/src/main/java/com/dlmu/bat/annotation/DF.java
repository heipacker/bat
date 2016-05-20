package com.dlmu.bat.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface DF {
}
