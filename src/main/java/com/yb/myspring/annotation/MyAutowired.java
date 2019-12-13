package com.yb.myspring.annotation;

import java.lang.annotation.*;

/**
 * 自定义Autowired注解
 *
 * @author YB
 * @date 2019-01-01
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {

    String value() default "";

}
