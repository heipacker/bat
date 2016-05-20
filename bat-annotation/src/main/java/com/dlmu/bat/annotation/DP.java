package com.dlmu.bat.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface DP {
}
