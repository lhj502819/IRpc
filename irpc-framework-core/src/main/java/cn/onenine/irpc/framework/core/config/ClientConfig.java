package cn.onenine.irpc.framework.core.config;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 12:54
 */
public class ClientConfig {

    private Integer port;

    private String serverAddr;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }
}
