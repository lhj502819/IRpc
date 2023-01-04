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

    public ServiceWrapper(Object serverObj) {
        this.serverObj = serverObj;
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
