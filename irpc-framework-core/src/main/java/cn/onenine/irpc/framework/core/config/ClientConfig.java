package cn.onenine.irpc.framework.core.config;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 12:54
 */
public class ClientConfig {

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
}
