package cn.onenine.irpc.framework.core.client;

import cn.onenine.irpc.framework.core.common.RpcEncoder;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.RpcProtocol;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.common.utils.CommonUtils;
import cn.onenine.irpc.framework.core.filter.client.*;
import cn.onenine.irpc.framework.core.proxy.jdk.JDKProxyFactory;
import cn.onenine.irpc.framework.core.common.RpcDecoder;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.AbstractRegister;
import cn.onenine.irpc.framework.core.registy.zookeeper.ZookeeperRegister;
import cn.onenine.irpc.framework.core.router.IRouter;
import cn.onenine.irpc.framework.core.router.RandomRouterImpl;
import cn.onenine.irpc.framework.core.router.RotateRouterImpl;
import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import cn.onenine.irpc.framework.core.serialize.fastjson.FastJsonSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.hessian.HessianSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.jdk.JdkSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.kroy.KryoSerializeFactory;
import cn.onenine.irpc.framework.core.spi.jdk.ExtensionLoader;
import cn.onenine.irpc.framework.interfaces.DataService;
import com.alibaba.fastjson2.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.*;
import static cn.onenine.irpc.framework.core.common.constant.RpcConstants.*;
import static cn.onenine.irpc.framework.core.common.constant.RpcConstants.KRYO_SERIALIZE_TYPE;
import static cn.onenine.irpc.framework.core.spi.jdk.ExtensionLoader.EXTENSION_LOADER_CLASS_CACHE;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 12:54
 */
public class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    public static EventLoopGroup clientGroup = new NioEventLoopGroup();


    private AbstractRegister abstractRegister;

    private IRpcListenerLoader iRpcListenerLoader;

    private Bootstrap bootstrap = new Bootstrap();

    public Bootstrap getBootstrap() {
        return bootstrap;
    }


    /**
     * 客户端需要通过一个代理工厂获取被调用对象的代理对象，然后通过代理对象将数据放入发送队列
     * 最后有一个异步线程将发送队列内部的数据一个个地发送到服务端，并且等待服务端响应对应的数据结果
     */
    public RpcReference initClientApplication() {
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        bootstrap.group(clientGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel channel) throws Exception {
                //管道中初始化一些逻辑，这里包含了上边所说的编解码器和客户端响应类
                channel.pipeline().addLast(new RpcEncoder());
                channel.pipeline().addLast(new RpcDecoder());
                channel.pipeline().addLast(new ClientHandler());
            }
        });
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();

        CLIENT_CONFIG = PropertiesBootstrap.loadClientConfigFromLocal();
        RpcReference rpcReference = null;
        if ("javassist".equals(CLIENT_CONFIG.getProxyType())) {
//            rpcReference = new RpcReference(new )
        } else {
            rpcReference = new RpcReference(new JDKProxyFactory());
        }
        return rpcReference;
    }

    public void doSubscribeService(Class serviceBean) {
        if (abstractRegister == null) {
            try {
                //使用自定义的SPI机制加载配置
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registerClassMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registerClass = registerClassMap.get(CLIENT_CONFIG.getRegisterType());
                //实例化SPI对象
                abstractRegister = (AbstractRegister) registerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("registryServiceType unKnow, error is ", e);
            }
        }
        URL url = new URL();
        url.setApplicationName(CLIENT_CONFIG.getApplicationName());
        url.setServiceName(serviceBean.getName());
        url.addParameter("host", CommonUtils.getIpAddress());
        Map<String, String> result = abstractRegister.getServiceWeightMap(serviceBean.getName());
        URL_MAP.put(serviceBean.getName(), result);
        abstractRegister.subscribe(url);
    }

    /**
     * 开始和各个provider建立连接
     */
    public void doConnectServer() {
        for (URL providerURL : CommonClientCache.SUBSCRIBE_SERVICE_LIST) {
            List<String> providerIps = abstractRegister.getProviderIps(providerURL.getServiceName());
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerURL.getServiceName(), providerIp);
                } catch (InterruptedException e) {
                    LOGGER.error("[doConnectServer] connect fail", e);
                }
            }
            URL url = new URL();
            url.setServiceName(providerURL.getServiceName());
            url.addParameter("servicePath", providerURL.getServiceName() + "/provider");
            url.addParameter("providerIps", JSONObject.toJSONString(providerIps));
            abstractRegister.doAfterSubscribe(url);
        }
    }

    /**
     * 开启发送线程，专门从事将数据包发送给服务端，起到一个解耦的作用
     */
    private void startClient() {
        //请求发送任务交给单独的IO线程去负责，实现异步化，提升发送性能
        Thread asyncSendJob = new Thread(new AsyncSendJob());
        asyncSendJob.start();
    }

    /**
     * 异步发送信息任务
     */
    class AsyncSendJob implements Runnable {

        public AsyncSendJob() {
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //阻塞模式
                    RpcInvocation data = CommonClientCache.SEND_QUEUE.take();
                    //将RpcInvocation封装到RpcProtocol对象中，然后发送给服务端，这里正好对应了ServerHandler
                    RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(data));
                    ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(data);
                    channelFuture.channel().writeAndFlush(rpcProtocol);
                } catch (Exception e) {
                    LOGGER.error("client call error", e);
                }
            }
        }
    }


    public static void main(String[] args) throws Throwable {
        Client client = new Client();
        RpcReference rpcReference = client.initClientApplication();
        //初始化客户端配置，如路由策略
        client.initConfig();
        RpcReferenceWrapper<DataService> dataServiceRpcReferenceWrapper = new RpcReferenceWrapper<>();
        dataServiceRpcReferenceWrapper.setAimClass(DataService.class);
        dataServiceRpcReferenceWrapper.setGroup("test");
        dataServiceRpcReferenceWrapper.setToken("token-a");

        DataService dataService = rpcReference.get(dataServiceRpcReferenceWrapper);
        client.doSubscribeService(DataService.class);
        ConnectionHandler.setBootstrap(client.getBootstrap());
        //连接所有的Provider
        client.doConnectServer();
        client.startClient();
        for (int i = 0; i < 100; i++) {
            try {
                String result = dataService.sendData("test");
                System.out.println(result);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("client error ", e);
            }
        }

    }

    private void initConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //初始化路由策略
        EXTENSION_LOADER.loadExtension(IRouter.class);
        String routeStrategy = CLIENT_CONFIG.getRouteStrategy();
        LinkedHashMap<String, Class> iRouterMap = EXTENSION_LOADER_CLASS_CACHE.get(IRouter.class.getName());
        Class iRouterClass = iRouterMap.get(routeStrategy);
        if (iRouterClass == null) {
            throw new RuntimeException("no match routerStrategy for " + routeStrategy);
        }
        IROUTER = (IRouter) iRouterClass.newInstance();

        //初始化序列化方式
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        String serializeType = CLIENT_CONFIG.getClientSerialize();
        LinkedHashMap<String, Class> serializeTypeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeClass = serializeTypeMap.get(serializeType);
        if (serializeClass == null) {
            throw new RuntimeException("no match serialize type for " + serializeType);
        }
        CLIENT_SERIALIZE_FACTORY = (SerializeFactory) serializeClass.newInstance();

        //初始化过滤链
        EXTENSION_LOADER.loadExtension(IClientFilter.class);
        ClientFilterChain clientFilterChain = new ClientFilterChain();
        LinkedHashMap<String, Class> filterMap = EXTENSION_LOADER_CLASS_CACHE.get(IClientFilter.class.getName());
        for (String implClassName : filterMap.keySet()) {
            Class filterClass = filterMap.get(implClassName);
            if (filterClass == null) {
                throw new NullPointerException("no match client filter for " + implClassName);
            }
            clientFilterChain.addServerFilter((IClientFilter) filterClass.newInstance());
        }
        CLIENT_FILTER_CHAIN = clientFilterChain;
    }
}
