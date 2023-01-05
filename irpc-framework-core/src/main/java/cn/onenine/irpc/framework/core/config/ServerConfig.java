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

    /**
     * 序列化方式 hessian,kryo,dk,fastJson2
     */
    private String serverSerialize;

    /**
     * 注册中心类型
     */
    private String registerType;

    /**
     * 任务队列大小
     */
    private Integer serverQueueSize;

    /**
     * 处理业务线程数
     */
    private Integer serverBizThreadNums;

    public Integer getServerQueueSize() {
        return serverQueueSize;
    }

    public void setServerQueueSize(Integer serverQueueSize) {
        this.serverQueueSize = serverQueueSize;
    }

    public Integer getServerBizThreadNums() {
        return serverBizThreadNums;
    }

    public void setServerBizThreadNums(Integer serverBizThreadNums) {
        this.serverBizThreadNums = serverBizThreadNums;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getServerSerialize() {
        return serverSerialize;
    }

    public void setServerSerialize(String serverSerialize) {
        this.serverSerialize = serverSerialize;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
