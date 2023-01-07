package cn.onenine.irpc.framework.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Description：Client端自动装配
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 21:08
 */
public class IRpcClientAutoConfiguration implements BeanPostProcessor {

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        

    }
}
