package cn.onenine.irpc.framework.core.client;

import cn.onenine.irpc.framework.core.common.RpcEncoder;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.RpcProtocol;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.common.utils.CommonUtils;
import cn.onenine.irpc.framework.core.config.ClientConfig;
import cn.onenine.irpc.framework.core.filter.client.*;
import cn.onenine.irpc.framework.core.proxy.jdk.JDKProxyFactory;
import cn.onenine.irpc.framework.core.common.RpcDecoder;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.AbstractRegister;
import cn.onenine.irpc.framework.core.router.IRouter;
import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import cn.onenine.irpc.framework.interfaces.DataService;
import com.alibaba.fastjson2.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.*;
import static cn.onenine.irpc.framework.core.common.constant.RpcConstants.DEFAULT_DECODE_CHAR;
import static cn.onenine.irpc.framework.core.spi.ExtensionLoader.EXTENSION_LOADER_CLASS_CACHE;

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
    public RpcReference initClientApplication() throws Exception {
        return initClientApplication(PropertiesBootstrap.loadClientConfigFromLocal());
    }



    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    public RpcReference initClientApplication(ClientConfig clientConfig) throws Exception {
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        bootstrap.group(clientGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ByteBuf delimiter = Unpooled.copiedBuffer(DEFAULT_DECODE_CHAR.getBytes());
                channel.pipeline().addLast(new DelimiterBasedFrameDecoder(CLIENT_CONFIG.getMaxServerRespDataSize(), delimiter));
                //????????????????????????????????????????????????????????????????????????????????????????????????
                channel.pipeline().addLast(new RpcEncoder());
                channel.pipeline().addLast(new RpcDecoder());
                channel.pipeline().addLast(new ClientHandler());
            }
        });
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        CLIENT_CONFIG = clientConfig;
        this.initConfig();
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
                //??????????????????SPI??????????????????
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registerClassMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registerClass = registerClassMap.get(CLIENT_CONFIG.getRegisterType());
                //?????????SPI??????
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
     * ???????????????provider????????????
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
     * ?????????????????????????????????????????????????????????????????????????????????????????????
     */
    public void startClient() {
        //?????????????????????????????????IO??????????????????????????????????????????????????????
        Thread asyncSendJob = new Thread(new AsyncSendJob());
        asyncSendJob.start();
    }

    /**
     * ????????????????????????
     */
    class AsyncSendJob implements Runnable {

        public AsyncSendJob() {
        }

        @Override
        public void run() {
            while (true) {
                RpcInvocation rpcInvocation = null;
                try {
                    rpcInvocation = CommonClientCache.SEND_QUEUE.take();
                    //????????????
                    ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(rpcInvocation);
                    //??????channel??????????????????
                    if (channelFuture.channel().isOpen()) {
                        //???RpcInvocation?????????RpcProtocol????????????????????????????????????????????????????????????ServerHandler
                        RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(rpcInvocation));
                        channelFuture.channel().writeAndFlush(rpcProtocol);
                    }
                } catch (Exception e) {
                    LOGGER.error("client call error", e);
                    rpcInvocation.setE(e);
                    RESP_MAP.put(rpcInvocation.getUuid(),rpcInvocation);
                }
            }
        }
    }




    private void initConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //?????????????????????
        EXTENSION_LOADER.loadExtension(IRouter.class);
        String routeStrategy = CLIENT_CONFIG.getRouteStrategy();
        LinkedHashMap<String, Class> iRouterMap = EXTENSION_LOADER_CLASS_CACHE.get(IRouter.class.getName());
        Class iRouterClass = iRouterMap.get(routeStrategy);
        if (iRouterClass == null) {
            throw new RuntimeException("no match routerStrategy for " + routeStrategy);
        }
        IROUTER = (IRouter) iRouterClass.newInstance();

        //????????????????????????
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        String serializeType = CLIENT_CONFIG.getClientSerialize();
        LinkedHashMap<String, Class> serializeTypeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeClass = serializeTypeMap.get(serializeType);
        if (serializeClass == null) {
            throw new RuntimeException("no match serialize type for " + serializeType);
        }
        CLIENT_SERIALIZE_FACTORY = (SerializeFactory) serializeClass.newInstance();

        //??????????????????
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
