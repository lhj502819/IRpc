package cn.onenine.irpc.framework.core.server;

/**
 * Description：服务包装类
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/2 19:20
 */
public class ServiceWrapper {

    /**
     * 对外暴露的具体服务对象
     */
    private Object serverObj;

    /**
     * 具体暴露服务的分组
     */
    private String group = "default";

    /**
     * 整个应用的token校验
     */
    private String serviceToken = "";

    /**
     * 限流策略
     */
    private Integer limit = -1;

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public ServiceWrapper(Object serverObj) {
        this.serverObj = serverObj;
    }

    public ServiceWrapper(Object serverObj, String group) {
        this.serverObj = serverObj;
        this.group = group;
    }

    public Object getServerObj() {
        return serverObj;
    }

    public void setServerObj(Object serverObj) {
        this.serverObj = serverObj;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
