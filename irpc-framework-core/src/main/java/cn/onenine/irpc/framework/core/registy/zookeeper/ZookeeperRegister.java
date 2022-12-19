package cn.onenine.irpc.framework.core.registy.zookeeper;

import cn.onenine.irpc.framework.core.common.event.IRpcEvent;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.common.event.IRpcUpdateEvent;
import cn.onenine.irpc.framework.core.common.event.data.URLChangeWrapper;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Description：负责对Zookeeper完成服务注册、服务订阅、服务下线等相关实际操作
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 12:02
 */
public class ZookeeperRegister extends AbstractRegister implements RegistryService {

    private static Logger logger = LoggerFactory.getLogger(ZookeeperRegister.class);

    private AbstractZookeeperClient zkClient;

    private String ROOT = "/irpc";

    public ZookeeperRegister(String address) {
        this.zkClient = new CuratorZookeeperClient(address);
    }

    @Override
    public void doAfterSubscribe(URL url) {
        String newServerNodePath = ROOT + "/" + url.getServiceName() + "/provider";
        //监听是否有新的服务注册
        watchChildNodeData(newServerNodePath);
    }

    private void watchChildNodeData(String newServerNodePath) {

        zkClient.watchChildNodeData(newServerNodePath, watchedEvent -> {
            logger.info("监听到事件：{}",watchedEvent);
            String path = watchedEvent.getPath();
            List<String> childrenDataList = zkClient.getChildrenData(path);
            URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
            urlChangeWrapper.setProviderUrl(childrenDataList);
            urlChangeWrapper.setServiceName(path.split("/")[2]);

            //自定义的事件监听组件
            //当某个节点的数据发生更新后，会发送一个节点更新的事件，然后在事件的监听端对不同的行为做不同的事件处理操作
            IRpcEvent iRpcEvent = new IRpcUpdateEvent(urlChangeWrapper);
            IRpcListenerLoader.sendEvent(iRpcEvent);

            //zk节点的消息通常只具有一次性的功效，所以可能会出现第一次修改节点之后发送一次通知，之后再次修改节点之后不会再发送节点变更通知
            //因此收到回调之后，需要在注册一次监听，这样能保证一直都有收到消息
            watchChildNodeData(path);
        });

    }

    @Override
    public List<String> getProviderIps(String serviceName) {
        return this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
    }

    @Override
    public void register(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }

        String urlStr = URL.buildProviderUrlStr(url);
        if (!zkClient.existNode(getProviderPath(url))) {
            zkClient.createTemporaryData(getProviderPath(url),urlStr);
        }else {
            zkClient.deleteNode(getConsumerPath(url));
            zkClient.createTemporarySeqData(getConsumerPath(url),urlStr);
        }
        super.subscribe(url);
    }

    private String getProviderPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/provider/" +
                url.getParameters().get("host") + ":" + url.getParameters().get("port");
    }

    private String getConsumerPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/consumer/" +
                url.getApplicationName() + ":" + url.getParameters().get("host") + ":";
    }

    @Override
    public void doUnSubscribe(URL url) {
        zkClient.deleteNode(getConsumerPath(url));
        super.doUnSubscribe(url);
    }
}
