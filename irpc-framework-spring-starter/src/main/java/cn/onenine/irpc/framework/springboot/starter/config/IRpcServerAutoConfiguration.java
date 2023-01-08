package cn.onenine.irpc.framework.springboot.starter.config;

import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcService;
import cn.onenine.irpc.framework.core.server.ApplicationShutdownHook;
import cn.onenine.irpc.framework.core.server.Server;
import cn.onenine.irpc.framework.core.server.ServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * Description：Server端自动装配
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 20:53
 */
public class IRpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRpcServerAutoConfiguration.class);

    private ApplicationContext applicationContext;

    public void afterPropertiesSet() throws Exception {
        Server server = null;
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(IRpcService.class);
        if (beansWithAnnotation.isEmpty()) {
            //没有带暴露的服务
            return;
        }

        printBanner();
        long start = System.currentTimeMillis();
        server = new Server();
        server.initServerConfig();
        IRpcListenerLoader iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        for (String beanName : beansWithAnnotation.keySet()) {
            Object beanObject = beansWithAnnotation.get(beanName);
            IRpcService iRpcService = beanObject.getClass().getAnnotation(IRpcService.class);
            ServiceWrapper serviceWrapper = new ServiceWrapper(beanObject, iRpcService.group());
            serviceWrapper.setLimit(iRpcService.limit());
            serviceWrapper.setServiceToken(iRpcService.serviceToken());
            server.exportService(serviceWrapper);
            LOGGER.info("service {} export success!", beanName);
        }
        long end = System.currentTimeMillis();
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
        LOGGER.info("{} start success in {} times", server.getConfig(), end - start);
    }

    private void printBanner() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("|||---------- IRpc Starting Now! ----------|||");
        System.out.println("==============================================");
        System.out.println("源代码地址: https://github.com/lhj502819/IRpc");
        System.out.println("version: 1.0.0");
        System.out.println();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
