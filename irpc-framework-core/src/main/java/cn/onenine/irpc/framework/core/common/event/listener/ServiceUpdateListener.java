package cn.onenine.irpc.framework.core.common.event.listener;

import cn.hutool.core.collection.CollectionUtil;
import cn.onenine.irpc.framework.core.client.ConnectionHandler;
import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.event.IRpcUpdateEvent;
import cn.onenine.irpc.framework.core.common.event.data.URLChangeWrapper;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description：服务变更listener，客户端更新本地的目标服务列表，避免向无用的服务发送请求
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 14:40
 */
public class ServiceUpdateListener implements IRpcListener<IRpcUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUpdateListener.class);

    @Override
    public void callBack(Object t) {

        URLChangeWrapper urlChangeWrapper = (URLChangeWrapper) t;
        List<ChannelFutureWrapper> channelFutureWrappers = CommonClientCache.CONNECT_MAP.get(urlChangeWrapper.getServiceName());
        if (CollectionUtil.isEmpty(channelFutureWrappers)) {
            LOGGER.error("[ServiceUpdateListener] channelFutureWrapper is empty");
            return;
        } else {
            //最新的provider
            List<String> matchProviderUrl = urlChangeWrapper.getProviderUrl();
            Set<String> finalUrl = new HashSet<>();
            List<ChannelFutureWrapper> finalChannelFutureWrappers = new ArrayList<>();
            /**
             * 移除老的URL
             */
            for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
                String oldServiceAddress = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
                //如果老的url没有，说明已经被移除了
                if (!matchProviderUrl.contains(oldServiceAddress)) {
                    continue;
                } else {
                    finalChannelFutureWrappers.add(channelFutureWrapper);
                    finalUrl.add(oldServiceAddress);
                }
            }

            /**
             * 增加新的
             */
            //此时老的已经被移除，开始检查是否有新的url
            //ChannelFutureWrapper是一个自定义的保证类，将netty建立好的ChannelFuture做了一些封装
            List<ChannelFutureWrapper> newChannelFutureWrapper = new ArrayList<>();
            for (String newProviderUrl : matchProviderUrl) {
                if (!finalUrl.contains(newProviderUrl)) {
                    //新的url
                    ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
                    String host = newProviderUrl.split(":")[0];
                    Integer port = Integer.valueOf(newProviderUrl.split(":")[1]);
                    channelFutureWrapper.setPort(port);
                    channelFutureWrapper.setHost(host);
                    ChannelFuture channelFuture = null;
                    try {
                        channelFuture = ConnectionHandler.createChannelFuture(host, port);
                        channelFutureWrapper.setChannelFuture(channelFuture);
                        newChannelFutureWrapper.add(channelFutureWrapper);
                        finalUrl.add(newProviderUrl);
                    } catch (Exception e) {
                        LOGGER.error("ServiceUpdateListener callback error", e);
                    }
                }
            }

            finalChannelFutureWrappers.addAll(newChannelFutureWrapper);
            //最终更新服务在这里
            CommonClientCache.CONNECT_MAP.put(urlChangeWrapper.getServiceName(),finalChannelFutureWrappers);
        }
    }
}
