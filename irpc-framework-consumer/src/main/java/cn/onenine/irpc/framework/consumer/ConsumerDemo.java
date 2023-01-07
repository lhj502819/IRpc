package cn.onenine.irpc.framework.consumer;

import cn.onenine.irpc.framework.core.client.Client;
import cn.onenine.irpc.framework.core.client.ConnectionHandler;
import cn.onenine.irpc.framework.core.client.RpcReference;
import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;
import cn.onenine.irpc.framework.interfaces.DataService;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.CLIENT_CONFIG;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/5 21:52
 */
public class ConsumerDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemo.class);

    public static void main(String[] args) throws Throwable {
        Client client = new Client();
        RpcReference rpcReference = client.initClientApplication();
        RpcReferenceWrapper<DataService> dataServiceRpcReferenceWrapper = new RpcReferenceWrapper<>();
        dataServiceRpcReferenceWrapper.setAimClass(DataService.class);
        dataServiceRpcReferenceWrapper.setGroup("test");
        dataServiceRpcReferenceWrapper.setToken("token-a");
        dataServiceRpcReferenceWrapper.setAsync(false);
        dataServiceRpcReferenceWrapper.setTimeOut(CLIENT_CONFIG.getCallTimeout());

        DataService dataService = rpcReference.get(dataServiceRpcReferenceWrapper);
        client.doSubscribeService(DataService.class);
        ConnectionHandler.setBootstrap(client.getBootstrap());
        //连接所有的Provider
        client.doConnectServer();
        client.startClient();
        for (int i = 0; i < 100; i++) {
            try {
                String result = dataService.sendData("test");
                System.out.println(result);
                System.out.println("=============");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("client error ", e);
            }
        }

    }
}
