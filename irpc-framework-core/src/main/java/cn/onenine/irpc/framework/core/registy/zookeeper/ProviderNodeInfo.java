package cn.onenine.irpc.framework.core.registy.zookeeper;

/**
 * Description：Provider节点信息
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 20:13
 */
public class ProviderNodeInfo {

    private String serviceName;

    private String address;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
