package cn.onenine.irpc.framework.core.registy;

import cn.onenine.irpc.framework.core.registy.zookeeper.ProviderNodeInfo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Description：配置总线，将IRPC的主要配置都封装在了里面，所有的重要的配置后期都是基于这个类去存储的
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 20:01
 */
public class URL {

    /**
     * 服务应用名称
     */
    private String applicationName;

    /**
     * 注册节点的服务名称，例如：cn.onenine.irpc.test.UserService
     */
    private String serviceName;

    /**
     * 这里面可以自定义不限进行扩展
     * 分组
     * 权重
     * 服务提供者的地址
     * 服务提供者的端口
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    public void addParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * 将URL转换为写入zk的provider节点下的一段字符串
     */
    public static String buildProviderUrlStr(URL url) {
        String host = url.getParameters().get("host");
        String port = url.getParameters().get("port");
        return new String((url.getApplicationName() + ";" +
                url.getServiceName() + ";" + host + ":" + port + ";" +
                System.currentTimeMillis()).getBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 将URL转换为写入consumer下的一段字符串
     *
     * @param url
     * @return
     */
    public static String buildConsumerUrlStr(URL url) {
        {
            String host = url.getParameters().get("host");
            return new String((url.getApplicationName() + ";" +
                    url.getServiceName() + ";" + host + ";" +
                    System.currentTimeMillis()).getBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 将某个节点下的信息转换为一个Provider节点对象
     */
    public static ProviderNodeInfo buildURLFromUrlStr(String providerNodeStr){
        String[] items = providerNodeStr.split("/");
        ProviderNodeInfo providerNodeInfo = new ProviderNodeInfo();
        providerNodeInfo.setServiceName(items[2]);
        providerNodeInfo.setAddress(items[4]);
        return providerNodeInfo;
    }

}
