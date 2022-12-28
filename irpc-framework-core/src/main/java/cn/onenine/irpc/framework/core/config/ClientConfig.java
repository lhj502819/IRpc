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
    private long callTimeout = 3000;

    private String routeStrategy;

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

    public long getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(long callTimeout) {
        this.callTimeout = callTimeout;
    }
}
