package cn.onenine.irpc.framework.core.registy.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Description：基于Curator实现的Zookeeper Client封装
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 22:44
 */
public class CuratorZookeeperClient extends AbstractZookeeperClient {

    private static Logger logger = LoggerFactory.getLogger(CuratorZookeeperClient.class);

    private CuratorFramework client;

    public CuratorZookeeperClient(String zkAddress) {
        this(zkAddress, null, null);
    }

    public CuratorZookeeperClient(String zkAddress, Integer baseSleepTimes, Integer maxRetryTimes) {
        super(zkAddress, baseSleepTimes, maxRetryTimes);
        //重试策略
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(super.getBaseSleepTimes(), super.getMaxRetryTimes());
        if (client == null) {
            client = CuratorFrameworkFactory.newClient(zkAddress, retryPolicy);
            client.start();
        }
    }

    public void updateNodeData(String address, String data) {
        try {
            client.setData().forPath(address, data.getBytes());
        } catch (Exception e) {
            logger.error("update node error", e);
        }
    }

    public Object getClient() {
        return client;
    }


    public String getNodeData(String address) {
        try {
            byte[] result = client.getData().forPath(address);
            if (result != null) {
                return new String(result);
            }
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            logger.error("get node data error", e);
        }
        return null;
    }

    public List<String> getChildrenData(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            logger.error("get children data error", e);
        }
        return null;
    }

    public void createPersistentData(String address, String data) {
        try {
            client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(address,data.getBytes());
        } catch (Exception e) {
            logger.error("create persistent data error", e);
        }
    }

    public void createPersistentWithSeqData(String address, String data) {

    }

    public void createTemporarySeqDat(String address, String data) {

    }

    public void createTemporaryData(String address, String data) {

    }

    public void setTemporaryData(String address, String data) {

    }

    public void destroy() {

    }

    public List<String> listNode(String address) {
        return null;
    }

    public boolean existNode(String address) {
        return false;
    }

    public void watchNodeData(String path, Watcher watcher) {

    }

    public void watchChildNodeData(String path, Watcher watcher) {

    }
}
