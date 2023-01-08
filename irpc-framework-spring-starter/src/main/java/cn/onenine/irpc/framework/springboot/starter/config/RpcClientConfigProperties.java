package cn.onenine.irpc.framework.springboot.starter.config;

import cn.onenine.irpc.framework.core.config.ClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * Description：Client配置
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 10:06
 */
//@Configuration
//@ConfigurationProperties(prefix = "irpc.client")
public class RpcClientConfigProperties {

    private String applicationName;

    private String registerAddr;

    private String proxyType;

    /**
     * 接口调用超时时间
     */
    private String callTimeout = "3000";

    private String routeStrategy;

    /**
     * 序列化方式 hessian,kryo,dk,fastJson2
     */
    private String clientSerialize;

    /**
     * 注册中心类型
     */
    private String registerType;

    /**
     * 最大接收的server端响应数据大小
     */
    private Integer maxServerRespDataSize;

    public Integer getMaxServerRespDataSize() {
        return maxServerRespDataSize;
    }

    public void setMaxServerRespDataSize(Integer maxServerRespDataSize) {
        this.maxServerRespDataSize = maxServerRespDataSize;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getClientSerialize() {
        return clientSerialize;
    }

    public void setClientSerialize(String clientSerialize) {
        this.clientSerialize = clientSerialize;
    }

    public String getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(String routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRegisterAddr() {
        return registerAddr;
    }

    public void setRegisterAddr(String registerAddr) {
        this.registerAddr = registerAddr;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public int getCallTimeout() {
        return Integer.parseInt(callTimeout);
    }

    public void setCallTimeout(String callTimeout) {
        this.callTimeout = callTimeout;
    }

    public void getClientConfig(){
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setApplicationName(applicationName);
        clientConfig.setRegisterAddr(registerAddr);
        clientConfig.setProxyType(proxyType);
        clientConfig.setCallTimeout(callTimeout);
        clientConfig.setRouteStrategy(routeStrategy);
        clientConfig.setClientSerialize(clientSerialize);
        clientConfig.setRegisterType(registerType);
        clientConfig.setMaxServerRespDataSize(maxServerRespDataSize);
    }

}
