package com.yb.myspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义RequestMapping注解
 *
 * @author YB
 * @date 2019-01-01
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {

    String value() default "";

}
