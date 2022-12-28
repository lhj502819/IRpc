package cn.onenine.irpc.framework.core.common.cache;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.config.ClientConfig;
import cn.onenine.irpc.framework.core.registy.URL;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Description：公共缓存，存储请求队列等公共信息
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:33
 */
public class CommonClientCache {

    public static BlockingQueue<RpcInvocation> SEND_QUEUE = new ArrayBlockingQueue<RpcInvocation>(100);

    public static Map<String,Object> RESP_MAP = new ConcurrentHashMap<String, Object>();

    /**
     * provider名称，该服务有哪些集群URL
     */
    public static List<String> SUBSCRIBE_SERVICE_LIST = new CopyOnWriteArrayList<>();

    public static ClientConfig CLIENT_CONFIG;

    public static Map<String,List<URL>> URL_MAP = new ConcurrentHashMap<>();

    public static Set<String> SERVER_ADDRESS = new HashSet<>();

    //每次进行远程调用的时候都是从这里面去选择服务提供者
    public static Map<String,List<ChannelFutureWrapper>> CONNECT_MAP = new ConcurrentHashMap<>();

}
