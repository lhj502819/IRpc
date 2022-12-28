package cn.onenine.irpc.framework.core.common.config;

import cn.onenine.irpc.framework.core.config.ClientConfig;
import cn.onenine.irpc.framework.core.config.ServerConfig;

import java.io.IOException;
import java.util.Objects;

/**
 * 主要负责将properties的配置转换成本地的一个Map结构进行管理
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 21:18
 */
public class PropertiesBootstrap {

    private volatile boolean configIsReady;

    public static final String SERVER_PORT = "irpc.serverPort";

    public static final String REGISTER_ADDRESS = "irpc.registerAddr";

    public static final String APPLICATION_NAME = "irpc.applicationName";

    public static final String PROXY_TYPE = "irpc.proxyType";

    public static final String CALL_TIMEOUT = "irpc.call.timeout";

    public static final String ROUTE_STRATEGY = "irpc.routerStrategy";

    public static ServerConfig loadServerConfigFromLocal(){
        try {
            PropertiesLoader.loadConfiguration();;
        }catch (IOException e){
            throw new RuntimeException("loadServerConfigFromLocal fail,e is  {}",e);
        }

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PropertiesLoader.getPropertiesInteger(SERVER_PORT));
        serverConfig.setApplicationName(PropertiesLoader.getPropertiesStr(APPLICATION_NAME));
        serverConfig.setRegisterAddr(PropertiesLoader.getPropertiesStr(REGISTER_ADDRESS));

        return serverConfig;
    }

    public static ClientConfig loadClientConfigFromLocal(){
        try {
            PropertiesLoader.loadConfiguration();;
        }catch (IOException e){
            throw new RuntimeException("loadClientConfigFromLocal fail,e is {}" , e);
        }

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setApplicationName(PropertiesLoader.getPropertiesStr(APPLICATION_NAME));
        clientConfig.setRegisterAddr(PropertiesLoader.getPropertiesStr(REGISTER_ADDRESS));
        clientConfig.setProxyType(PropertiesLoader.getPropertiesStr(PROXY_TYPE));
        clientConfig.setCallTimeout(Long.parseLong(Objects.requireNonNull(PropertiesLoader.getPropertiesStr(CALL_TIMEOUT))));
        clientConfig.setRouteStrategy(PropertiesLoader.getPropertiesStr(ROUTE_STRATEGY));
        return clientConfig;
    }
}
