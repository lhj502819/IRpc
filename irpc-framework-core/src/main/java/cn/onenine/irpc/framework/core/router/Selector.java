package cn.onenine.irpc.framework.core.router;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/27 22:08
 */
public class Selector {

    /**
     * 服务命名
     * 如：cn.onenine.user.UserService
     */
    private String providerServiceName;

    public String getProviderServiceName() {
        return providerServiceName;
    }

    public void setProviderServiceName(String providerServiceName) {
        this.providerServiceName = providerServiceName;
    }
}
