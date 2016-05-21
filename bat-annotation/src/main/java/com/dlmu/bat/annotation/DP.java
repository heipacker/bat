package com.dlmu.bat.annotation;

import java.lang.annotation.*;

/**
 * 注解一个参数, 如果要把参数放到trace的context里面
 * paramName=paramValue
 *
 * @author heipacker
 * @date 16-4-2
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface DP {
}
