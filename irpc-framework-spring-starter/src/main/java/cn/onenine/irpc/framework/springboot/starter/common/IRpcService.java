package cn.onenine.irpc.framework.springboot.starter.common;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Description：标记该注解的类会被Spring自动发现
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 20:50
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface IRpcService {

    int limit() default 0;

    String group() default "";

    String serviceToken() default "";

}
