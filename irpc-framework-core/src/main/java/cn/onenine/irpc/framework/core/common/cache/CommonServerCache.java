package cn.onenine.irpc.framework.core.common.cache;

import cn.onenine.irpc.framework.core.common.ServerServiceSemaphoreWrapper;
import cn.onenine.irpc.framework.core.config.ServerConfig;
import cn.onenine.irpc.framework.core.dispatcher.ServerChannelDispatcher;
import cn.onenine.irpc.framework.core.filter.server.ServerFilterChain;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import cn.onenine.irpc.framework.core.server.ServiceWrapper;
import cn.onenine.irpc.framework.core.spi.ExtensionLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/16 23:04
 */
public class CommonServerCache {

    public static Map<String,Object> PROVIDER_CLASS_MAP = new ConcurrentHashMap<String, Object>();

    public static Set<URL> PROVIDER_URL_SET = new CopyOnWriteArraySet<>();

    public static RegistryService REGISTRY_SERVICE;

    public static SerializeFactory SERVER_SERIALIZE_FACTORY;

    /**
     * 前置过滤器链
     */
    public static ServerFilterChain SERVER_BEFORE_FILTER_CHAIN = new ServerFilterChain();

    /**
     * 后置过滤器链
     */
    public static ServerFilterChain SERVER_AFTER_FILTER_CHAIN = new ServerFilterChain();

    /**
     * key：serviceName value：ServiceWrapper
     */
    public static Map<String, ServiceWrapper> PROVIDER_SERVICE_WRAPPER_MAP = new HashMap<>();

    /**
     * SPI
     */
    public static ExtensionLoader EXTENSION_LOADER = new ExtensionLoader();

    public static ServerConfig SERVER_CONFIG;

    public static ServerChannelDispatcher SERVER_CHANNEL_DISPATCHER = new ServerChannelDispatcher();


    public static final Map<String,ServerServiceSemaphoreWrapper> SERVER_SERVICE_SEMAPHORE_MAP = new ConcurrentHashMap<>(64);



}
