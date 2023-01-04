package cn.onenine.irpc.framework.core.client;

import cn.hutool.core.collection.CollectionUtil;
import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.ProviderNodeInfo;
import cn.onenine.irpc.framework.core.router.Selector;
import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;

import java.util.*;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.*;

/**
 * Description：将连接的建立、断开、按照服务名筛选等功能都封装在了一起，
 * 按照单一指责的设计原则，将与连接有关的功能都统一封装在了一起
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 17:06
 */
public class ConnectionHandler {

    /**
     * 核心的连接处理器
     * 专门用于负责和服务端构建连接通信
     */
    private static Bootstrap bootstrap;

    public static void setBootstrap(Bootstrap bootstrap) {
        ConnectionHandler.bootstrap = bootstrap;
    }

    /**
     * 构建单个连接通道，元操作
     *
     * @param providerServiceName
     * @param providerIpAndPort
     * @throws InterruptedException
     */
    public static void connect(String providerServiceName, String providerIpAndPort) throws InterruptedException {
        if (bootstrap == null) {
            throw new RuntimeException("bootstrap can not be null");
        }

        //格式错误类型的信息
        if (!providerIpAndPort.contains(":")) {
            return;
        }
        String[] providerAddress = providerIpAndPort.split(":");
        String ip = providerAddress[0];
        int port = Integer.parseInt(providerAddress[1]);
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
        channelFutureWrapper.setChannelFuture(channelFuture);
        channelFutureWrapper.setHost(ip);
        channelFutureWrapper.setPort(port);
        String providerUrlInfo = URL_MAP.get(providerServiceName).get(providerIpAndPort);
        ProviderNodeInfo providerNodeInfo = URL.buildURLFromUrlStr(providerUrlInfo);
        //设置该Channel的权重值
        channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
        channelFutureWrapper.setGroup(providerNodeInfo.getGroup());
        SERVER_ADDRESS.add(providerIpAndPort);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CollectionUtil.isEmpty(channelFutureWrappers)) {
            channelFutureWrappers = new ArrayList<>();
        }

        channelFutureWrappers.add(channelFutureWrapper);
        //例如com.sise.test.UserService会被放入到一个Map集合中，key是服务的名字，value是对应的channel通道的List集合
        CONNECT_MAP.put(providerServiceName, channelFutureWrappers);

        Selector selector = new Selector();
        selector.setProviderServiceName(providerServiceName);
        IROUTER.refreshRouterArr(selector);
    }


    public static ChannelFuture createChannelFuture(String host, Integer port) throws InterruptedException {
        return bootstrap.connect(host, port).sync();
    }

    /**
     * 断开连接
     *
     * @param providerServiceName 服务名
     * @param providerIp          服务提供者IP
     */
    public static void disConnect(String providerServiceName, String providerIp) {
        SERVER_ADDRESS.remove(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CollectionUtil.isNotEmpty(channelFutureWrappers)) {
            channelFutureWrappers.removeIf(channelFutureWrapper ->
                    providerIp.equals(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort()));
        }
    }

    /**
     * 默认走随机策略获取ChannelFuture
     */
    public static ChannelFuture getChannelFuture(RpcInvocation rpcInvocation) {
        ChannelFutureWrapper[] channelFutureWrappers = SERVICE_ROUTER_MAP.get(rpcInvocation.getTargetServiceName());
        if (channelFutureWrappers == null || channelFutureWrappers.length == 0) {
            throw new RuntimeException("no provider exist for " + rpcInvocation.getTargetServiceName());
        }

        ArrayList<ChannelFutureWrapper> channelFutureWrappersCopy = Lists.newArrayList(channelFutureWrappers);
        //doFilter
        CLIENT_FILTER_CHAIN.doFilter(channelFutureWrappersCopy, rpcInvocation);

        Selector selector = new Selector();
        selector.setProviderServiceName(rpcInvocation.getTargetServiceName());
        ChannelFutureWrapper[] channelFutureWrappersArrCopy = new ChannelFutureWrapper[channelFutureWrappersCopy.size()];
        selector.setChannelFutureWrappers(channelFutureWrappersCopy.toArray(channelFutureWrappersArrCopy));
        //通过指定的路由算法选择一个Provider ChannelFuture
        return IROUTER.select(selector).getChannelFuture();
    }
}
