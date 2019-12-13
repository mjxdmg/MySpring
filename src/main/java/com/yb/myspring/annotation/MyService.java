package com.yb.myspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义Service注解
 *
 * @author YB
 * @date 2019-01-01
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {

    String value() default "";

}
