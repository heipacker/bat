package com.dlmu.bat.annotation;

import java.lang.annotation.*;

/**
 * 如果需要把一个field添加到trace的context里面
 * fieldName=fieldValue
 *
 * @author heipacker
 * @date 16-4-2
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface DF {
}
