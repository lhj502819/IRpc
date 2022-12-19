package cn.onenine.irpc.framework.core.common.event.data;

import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 13:36
 */
public class URLChangeWrapper {

    private String serviceName;

    private List<String> providerUrl;


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<String> getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(List<String> providerUrl) {
        this.providerUrl = providerUrl;
    }

    @Override
    public String toString() {
        return "URLChangeWrapper{" +
                "serviceName='" + serviceName + '\'' +
                ", providerUrl=" + providerUrl +
                '}';
    }
}
