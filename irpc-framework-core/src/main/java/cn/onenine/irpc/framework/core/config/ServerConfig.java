package cn.onenine.irpc.framework.core.config;

/**
 * Description： Server配置类
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/16 23:16
 */
public class ServerConfig {

    private int port;

    private String applicationName;

    private String registerAddr;

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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
