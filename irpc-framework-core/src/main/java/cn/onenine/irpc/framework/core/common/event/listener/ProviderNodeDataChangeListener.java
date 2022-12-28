package cn.onenine.irpc.framework.core.common.event.listener;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.event.IRpcNodeChangeEvent;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.ProviderNodeInfo;

import java.util.List;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.CONNECT_MAP;
import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.IROUTER;

/**
 * Description：Provider节点数据变更事件监听者
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/28 22:13
 */
public class ProviderNodeDataChangeListener implements IRpcListener<IRpcNodeChangeEvent> {
    @Override
    public void callBack(Object t) {
        ProviderNodeInfo providerNodeInfo = (ProviderNodeInfo) t;
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerNodeInfo.getServiceName());
        for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
            String address = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
            if (address.equals(providerNodeInfo.getAddress())){
                //修改权重值
                channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
                URL url = new URL();
                url.setServiceName(providerNodeInfo.getServiceName());
                IROUTER.updateWeight(url);
            }
        }
    }
}
