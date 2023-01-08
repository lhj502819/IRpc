package cn.onenine.irpc.framework.springboot.starter.common;

import java.lang.annotation.*;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 20:47
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IRpcReference {

    String url() default "";

    String group() default "";

    String serviceToken() default "";

    int timeOut() default 3000;

    int retry() default 1;

    boolean async() default false;
}
