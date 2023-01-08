package cn.onenine.irpc.framework.springboot.starter.config;

import cn.onenine.irpc.framework.springboot.starter.common.IRpcReference;
import cn.onenine.irpc.framework.core.client.Client;
import cn.onenine.irpc.framework.core.client.ConnectionHandler;
import cn.onenine.irpc.framework.core.client.RpcReference;
import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;

/**
 * Description：Client端自动装配
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 21:08
 */
//@ConditionalOnProperty(prefix = "irpc.client", name = "enabled", matchIfMissing = true)
//@EnableConfigurationProperties(RpcClientConfigProperties.class)
public class IRpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRpcClientAutoConfiguration.class);

    private Client client;

    private RpcReference rpcReference;

    /**
     * 是否需要启动NettyClient
     */
    private boolean hasInitClientApplication;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                //设置私有变量可访问
                field.setAccessible(true);
                IRpcReference rpcReferenceAnnotation = field.getAnnotation(IRpcReference.class);
                if (rpcReferenceAnnotation == null) {
                    continue;
                }
                if (!hasInitClientApplication){
                    try {
                        client = new Client();
                        rpcReference = client.initClientApplication();
                    } catch (Exception e) {
                        LOGGER.error("init and start netty client error" , e);
                        throw new RuntimeException(e);
                    }
                }
                hasInitClientApplication = true;
                RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                rpcReferenceWrapper.setAimClass(field.getType());
                rpcReferenceWrapper.setGroup(rpcReferenceAnnotation.group());
                rpcReferenceWrapper.setTimeOut(rpcReferenceAnnotation.timeOut());
                rpcReferenceWrapper.setToken(rpcReferenceAnnotation.serviceToken());
                rpcReferenceWrapper.setUrl(rpcReferenceAnnotation.url());
                rpcReferenceWrapper.setRetry(rpcReferenceAnnotation.retry());
                rpcReferenceWrapper.setAsync(rpcReferenceAnnotation.async());
                field.set(bean,rpcReference.get(rpcReferenceWrapper));
                //订阅服务，提前获取到所有的service Provider
                client.doSubscribeService(field.getType());
            } catch (Throwable e) {
                throw new RuntimeException("[IRpcClientAutoConfiguration#postProcessAfterInitialization] init rpcReference error", e);
            }
        }
        return bean;
    }

    public void onApplicationEvent(ApplicationReadyEvent event) {
       if (hasInitClientApplication){
           ConnectionHandler.setBootstrap(client.getBootstrap());
           client.doConnectServer();
           client.startClient();
       }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println(applicationContext);
    }
}
