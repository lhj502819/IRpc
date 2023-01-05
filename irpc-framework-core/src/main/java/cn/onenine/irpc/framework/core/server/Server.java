package cn.onenine.irpc.framework.core.server;

import cn.hutool.core.util.StrUtil;
import cn.onenine.irpc.framework.core.common.RpcDecoder;
import cn.onenine.irpc.framework.core.common.RpcEncoder;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.common.utils.CommonUtils;
import cn.onenine.irpc.framework.core.config.ServerConfig;
import cn.onenine.irpc.framework.core.filter.server.IServerFilter;
import cn.onenine.irpc.framework.core.filter.server.ServerFilterChain;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.AbstractRegister;
import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.util.LinkedHashMap;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.*;
import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.EXTENSION_LOADER;
import static cn.onenine.irpc.framework.core.spi.ExtensionLoader.EXTENSION_LOADER_CLASS_CACHE;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/16 23:04
 */
public class Server {

    private static EventLoopGroup bossGroup = null;

    private static EventLoopGroup workerGroup = null;

    private static IRpcListenerLoader iRpcListenerLoader;


    public ServerConfig getServerConfig() {
        return SERVER_CONFIG;
    }

    public void startApplication() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootStrap = new ServerBootstrap();
        bootStrap.group(bossGroup, workerGroup);
        bootStrap.channel(NioServerSocketChannel.class);
        bootStrap.option(ChannelOption.TCP_NODELAY, true);
        bootStrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootStrap.option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true);

        bootStrap.childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel channel) throws Exception {
                System.out.println("初始化provider过程");
                channel.pipeline().addLast(new RpcEncoder());
                channel.pipeline().addLast(new RpcDecoder());
                channel.pipeline().addLast(new ServerHandler());
            }
        });

        this.batchExportUrl();

        bootStrap.bind(SERVER_CONFIG.getPort()).sync();
    }

    /**
     * 暴露服务信息
     */
    public void exportService(ServiceWrapper serviceWrapper){
        Object serviceBean = serviceWrapper.getServerObj();
        if (serviceBean.getClass().getInterfaces().length == 0){
            throw new RuntimeException("service must had interfaces!");
        }
        Class<?>[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length >1){
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (REGISTRY_SERVICE == null){
            try {
                //使用自定义的SPI机制加载配置
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registerClassMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registerClass = registerClassMap.get(SERVER_CONFIG.getRegisterType());
                //实例化SPI对象
                REGISTRY_SERVICE = (AbstractRegister) registerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("registryServiceType unKnow, error is ", e);
            }
        }
        //默认选择该对象的第一个实现
        Class<?> interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(),serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(SERVER_CONFIG.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port",String.valueOf(SERVER_CONFIG.getPort()));
        url.addParameter("group", String.valueOf(serviceWrapper.getGroup()));
        url.addParameter("limit",String.valueOf(serviceWrapper.getLimit()));
        PROVIDER_URL_SET.add(url);
        if (StrUtil.isNotBlank(serviceWrapper.getServiceToken())){
            PROVIDER_SERVICE_WRAPPER_MAP.put(interfaceClass.getName(),serviceWrapper);
        }

    }

    /**
     * 为了将服务端的具体访问都暴露到注册中心
     */
    private void batchExportUrl() {

        Thread task = new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (URL url : PROVIDER_URL_SET) {
                REGISTRY_SERVICE.register(url);
            }
        });

        task.start();

    }


    public void initServerConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        SERVER_CONFIG = PropertiesBootstrap.loadServerConfigFromLocal();
        //初始化序列化方式
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        String serializeType = SERVER_CONFIG.getServerSerialize();
        LinkedHashMap<String, Class> serializeTypeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeClass = serializeTypeMap.get(serializeType);
        if (serializeClass == null) {
            throw new RuntimeException("no match serialize type for " + serializeType);
        }
        SERVER_SERIALIZE_FACTORY = (SerializeFactory) serializeClass.newInstance();

        //初始化过滤链
        EXTENSION_LOADER.loadExtension(IServerFilter.class);
        ServerFilterChain serverFilterChain = new ServerFilterChain();
        LinkedHashMap<String, Class> filterMap = EXTENSION_LOADER_CLASS_CACHE.get(IServerFilter.class.getName());
        for (String implClassName : filterMap.keySet()) {
            Class filterClass = filterMap.get(implClassName);
            if (filterClass == null) {
                throw new NullPointerException("no match server filter for " + implClassName);
            }
            serverFilterChain.addServerFilter((IServerFilter) filterClass.newInstance());
        }
        SERVER_FILTER_CHAIN = serverFilterChain;
    }


    public static void main(String[] args) throws InterruptedException {
        try {
            Server server = new Server();
            server.initServerConfig();
            iRpcListenerLoader = new IRpcListenerLoader();
            iRpcListenerLoader.init();
            ServiceWrapper dataServiceWrapper = new ServiceWrapper(new DataServiceImpl(),"test");
            dataServiceWrapper.setServiceToken("token-a");
            dataServiceWrapper.setLimit(2);
            server.exportService(dataServiceWrapper);
            ServiceWrapper userServiceWrapper = new ServiceWrapper(new UserServiceImpl(),"dev");
            userServiceWrapper.setServiceToken("token-b");
            userServiceWrapper.setLimit(2);
            server.exportService(userServiceWrapper);
            //注册destroy钩子函数
            ApplicationShutdownHook.registryShutdownHook();
            server.startApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
