package cn.onenine.irpc.framework.core.common.cache;

import cn.onenine.irpc.framework.core.common.ChannelFuturePollingRef;
import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.config.ClientConfig;
import cn.onenine.irpc.framework.core.filter.client.ClientFilterChain;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.router.IRouter;
import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import cn.onenine.irpc.framework.core.spi.ExtensionLoader;

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

    public static Map<String,Object> RESP_MAP = new ConcurrentHashMap<>();

    /**
     * provider名称，该服务有哪些集群URL
     */
    public static List<URL> SUBSCRIBE_SERVICE_LIST = new CopyOnWriteArrayList<>();

    /**
     * key：serviceName value：(key:ip+port value:节点信息)
     */
    public static Map<String, Map<String, String>> URL_MAP = new ConcurrentHashMap<String, Map<String, String>>();

    public static Set<String> SERVER_ADDRESS = new HashSet<>();

    //每次进行远程调用的时候都是从这里面去选择服务提供者
    public static Map<String,List<ChannelFutureWrapper>> CONNECT_MAP = new ConcurrentHashMap<>();

    //随机请求的Map，key：serviceName，value：provider channel列表
    public static Map<String,ChannelFutureWrapper[]> SERVICE_ROUTER_MAP = new ConcurrentHashMap<>();
    //对SERVICE_ROUTER_MAP的取用进行了包装
    public static ChannelFuturePollingRef CHANNEL_FUTURE_POLLING_REF = new ChannelFuturePollingRef();

    /**
     * 根据配置进行初始化
     */
    public static IRouter IROUTER;

    /**
     * 客户端序列化方式
     */
    public static SerializeFactory CLIENT_SERIALIZE_FACTORY;

    /**
     * 客户端过滤器链
     */
    public static ClientFilterChain CLIENT_FILTER_CHAIN;

    /**
     * 客户端配置
     */
    public static ClientConfig CLIENT_CONFIG;

    /**
     * SPI
     */
    public static ExtensionLoader EXTENSION_LOADER = new ExtensionLoader();

}
