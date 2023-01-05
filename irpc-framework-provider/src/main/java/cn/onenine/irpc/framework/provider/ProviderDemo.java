package cn.onenine.irpc.framework.provider;

import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.server.*;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/5 21:51
 */
public class ProviderDemo {

    public static void main(String[] args) throws InterruptedException {
        try {
            Server server = new Server();
            server.initServerConfig();
            IRpcListenerLoader iRpcListenerLoader = new IRpcListenerLoader();
            iRpcListenerLoader.init();
            ServiceWrapper dataServiceWrapper = new ServiceWrapper(new DataServiceImpl(),"test");
            dataServiceWrapper.setServiceToken("token-a");
            dataServiceWrapper.setLimit(2);
            server.exportService(dataServiceWrapper);
            ServiceWrapper userServiceWrapper = new ServiceWrapper(new UserServiceImpl(),"dev");
            userServiceWrapper.setServiceToken("token-b");
            userServiceWrapper.setLimit(2);
            server.exportService(userServiceWrapper);
            //注册destroy钩子函数
            ApplicationShutdownHook.registryShutdownHook();
            server.startApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
