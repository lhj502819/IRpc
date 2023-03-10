package cn.onenine.irpc.framework.core.server;

import cn.hutool.core.util.StrUtil;
import cn.onenine.irpc.framework.core.common.RpcDecoder;
import cn.onenine.irpc.framework.core.common.RpcEncoder;
import cn.onenine.irpc.framework.core.common.ServerServiceSemaphoreWrapper;
import cn.onenine.irpc.framework.core.common.annotations.SPI;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.constant.RpcConstants;
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static EventLoopGroup bossGroup = null;

    private static EventLoopGroup workerGroup = null;

    private static IRpcListenerLoader iRpcListenerLoader;

    private ServerHandler serverHandler;

    private MaxConnectionLimitHandler maxConnectionLimitHandler;

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

        maxConnectionLimitHandler = new MaxConnectionLimitHandler(SERVER_CONFIG.getMaxConnections());
        bootStrap.handler(maxConnectionLimitHandler);

        serverHandler = new ServerHandler();

        bootStrap.childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel channel) throws Exception {
                LOGGER.info("?????????provider??????");
                ByteBuf delimiter = Unpooled.copiedBuffer(RpcConstants.DEFAULT_DECODE_CHAR.getBytes());
                channel.pipeline().addLast(new DelimiterBasedFrameDecoder(SERVER_CONFIG.getMaxServerRequestData(), delimiter));
                channel.pipeline().addLast(new RpcEncoder());
                channel.pipeline().addLast(new RpcDecoder());
                channel.pipeline().addLast(serverHandler);
            }
        });


        this.batchExportUrl();
        //????????????????????????
        SERVER_CHANNEL_DISPATCHER.startDataConsume();

        bootStrap.bind(SERVER_CONFIG.getPort()).sync();
        LOGGER.info("Server started..");
    }

    /**
     * ??????????????????
     */
    public void exportService(ServiceWrapper serviceWrapper) {
        Object serviceBean = serviceWrapper.getServerObj();
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class<?>[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (REGISTRY_SERVICE == null) {
            try {
                //??????????????????SPI??????????????????
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registerClassMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registerClass = registerClassMap.get(SERVER_CONFIG.getRegisterType());
                //?????????SPI??????
                REGISTRY_SERVICE = (AbstractRegister) registerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("registryServiceType unKnow, error is ", e);
            }
        }
        //???????????????????????????????????????
        Class<?> interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(SERVER_CONFIG.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port", String.valueOf(SERVER_CONFIG.getPort()));
        url.addParameter("group", String.valueOf(serviceWrapper.getGroup()));
        url.addParameter("limit", String.valueOf(serviceWrapper.getLimit()));

        //???????????????????????????
        SERVER_SERVICE_SEMAPHORE_MAP.put(interfaceClass.getName(),new ServerServiceSemaphoreWrapper(serviceWrapper.getLimit()));

        PROVIDER_URL_SET.add(url);
        if (StrUtil.isNotBlank(serviceWrapper.getServiceToken())) {
            PROVIDER_SERVICE_WRAPPER_MAP.put(interfaceClass.getName(), serviceWrapper);
        }

    }

    /**
     * ?????????????????????????????????????????????????????????
     */
    private void batchExportUrl() {

        Thread task = new Thread(() -> {
            try {
                Thread.sleep(1500);
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
        //????????????????????????????????????
        SERVER_CHANNEL_DISPATCHER.init(SERVER_CONFIG.getServerQueueSize(), SERVER_CONFIG.getServerBizThreadNums());
        //????????????????????????
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        String serializeType = SERVER_CONFIG.getServerSerialize();
        LinkedHashMap<String, Class> serializeTypeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeClass = serializeTypeMap.get(serializeType);
        if (serializeClass == null) {
            throw new RuntimeException("no match serialize type for " + serializeType);
        }
        SERVER_SERIALIZE_FACTORY = (SerializeFactory) serializeClass.newInstance();

        //??????????????????
        EXTENSION_LOADER.loadExtension(IServerFilter.class);
        ServerFilterChain serverBeforeFilterChain = new ServerFilterChain();
        ServerFilterChain serverAfterFilterChain = new ServerFilterChain();
        LinkedHashMap<String, Class> filterMap = EXTENSION_LOADER_CLASS_CACHE.get(IServerFilter.class.getName());
        for (String implClassName : filterMap.keySet()) {
            Class filterClass = filterMap.get(implClassName);
            if (filterClass == null) {
                throw new NullPointerException("no match server filter for " + implClassName);
            }
            Annotation spiAnnotation = filterClass.getDeclaredAnnotation(SPI.class);
            if (spiAnnotation == null) {
                LOGGER.warn("filter {}spi annotation is null ", filterClass.getName());
                continue;
            }
            SPI spi = (SPI) spiAnnotation;
            if ("before".equals(spi.value())) {
                serverBeforeFilterChain.addServerFilter((IServerFilter) filterClass.newInstance());
            } else if ("after".equals(spi.value())) {
                serverAfterFilterChain.addServerFilter((IServerFilter) filterClass.newInstance());
            }
        }
        SERVER_BEFORE_FILTER_CHAIN = serverBeforeFilterChain;
        SERVER_AFTER_FILTER_CHAIN = serverAfterFilterChain;
    }

    public ServerConfig getConfig(){
        return SERVER_CONFIG;
    }

}
