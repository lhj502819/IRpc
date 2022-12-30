package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.core.common.RpcDecoder;
import cn.onenine.irpc.framework.core.common.RpcEncoder;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import cn.onenine.irpc.framework.core.common.utils.CommonUtils;
import cn.onenine.irpc.framework.core.config.ServerConfig;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.registy.zookeeper.ZookeeperRegister;
import cn.onenine.irpc.framework.core.serialize.fastjson.FastJsonSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.hessian.HessianSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.jdk.JdkSerializeFactory;
import cn.onenine.irpc.framework.core.serialize.kroy.KryoSerializeFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.*;
import static cn.onenine.irpc.framework.core.common.constant.RpcConstants.*;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/16 23:04
 */
public class Server {

    private static EventLoopGroup bossGroup = null;

    private static EventLoopGroup workerGroup = null;

    private ServerConfig serverConfig;

    private static IRpcListenerLoader iRpcListenerLoader;


    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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

        bootStrap.bind(serverConfig.getPort()).sync();
    }

    /**
     * 暴露服务信息
     */
    public void exportService(Object serviceBean){
        if (serviceBean.getClass().getInterfaces().length == 0){
            throw new RuntimeException("service must had interfaces!");
        }
        Class<?>[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length >1){
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (REGISTRY_SERVICE == null){
            REGISTRY_SERVICE = new ZookeeperRegister(serverConfig.getRegisterAddr());
        }
        //默认选择该对象的第一个实现
        Class<?> interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(),serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(serverConfig.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port",String.valueOf(serverConfig.getPort()));
        PROVIDER_URL_SET.add(url);
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


    public void initServerConfig(){
        ServerConfig serverConfig = PropertiesBootstrap.loadServerConfigFromLocal();
        this.setServerConfig(serverConfig);
        String serverSerialize = serverConfig.getServerSerialize();
        switch (serverSerialize){
            case JDK_SERIALIZE_TYPE :
                SERVER_SERIALIZE_FACTORY = new JdkSerializeFactory();
                break;
            case HESSIAN2_SERIALIZE_TYPE:
                SERVER_SERIALIZE_FACTORY = new HessianSerializeFactory();
                break;
            case FAST_JSON_SERIALIZE_TYPE:
                SERVER_SERIALIZE_FACTORY = new FastJsonSerializeFactory();
                break;
            case KRYO_SERIALIZE_TYPE:
                SERVER_SERIALIZE_FACTORY = new KryoSerializeFactory();
                break;
            default:
                throw new RuntimeException("no match serialize type for " +serverSerialize);
        }
        System.out.println("serverSerialize is "+serverSerialize);
    }


    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        server.initServerConfig();
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        server.exportService(new DataServiceImpl());
        server.exportService(new UserServiceImpl());
        server.startApplication();
        //注册destroy钩子函数
        ApplicationShutdownHook.registryShutdownHook();
    }

}
