package cn.onenine.irpc.framework.core.dispatcher;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.RpcProtocol;
import cn.onenine.irpc.framework.core.common.cache.CommonServerCache;
import cn.onenine.irpc.framework.core.common.exception.IRpcException;
import io.netty.util.ReferenceCountUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.*;
import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.SERVER_SERIALIZE_FACTORY;

/**
 * Description：请求分发器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/5 21:15
 */
public class ServerChannelDispatcher {

    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;

    private ExecutorService executorService;

    public void init(int queueSize, int bizThreadNums) {
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(bizThreadNums, bizThreadNums,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(512));
    }

    public void add(ServerChannelReadData serverChannelReadData) {
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    public ServerChannelDispatcher() {
    }

    class ServerJobCoreHandle implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
                    executorService.submit(() -> {
                        try {
                            RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
                            RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);

                            try {
                                //doBeforeFilter 前置过滤器
                                SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInvocation);
                            } catch (Exception e) {
                                //针对自定义异常进行处理
                                if (e instanceof IRpcException){
                                    IRpcException rpcException = (IRpcException) e;
                                    RpcInvocation reqParam = rpcException.getRpcInvocation();

                                    byte[] body = SERVER_SERIALIZE_FACTORY.serialize(reqParam);
                                    RpcProtocol respRpcProtocol = new RpcProtocol(body);
                                    serverChannelReadData.getChannelHandler().writeAndFlush(respRpcProtocol);
                                    return;
                                }
                            }

                            //这里的PROVIDER_CLASS_MAP就是一开始预先在启动的时候存储的Bean集合
                            Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
                            Method[] methods = aimObject.getClass().getDeclaredMethods();
                            Object result = null;
                            for (Method method : methods) {
                                if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                                    if (method.getReturnType().equals(Void.TYPE)) {
                                        try {
                                            method.invoke(aimObject, rpcInvocation.getArgs());
                                        } catch (Exception e) {
                                            //业务异常返回给客户端
                                            rpcInvocation.setE(e);
                                        }
                                    } else {
                                        try {
                                            result = method.invoke(aimObject, rpcInvocation.getArgs());
                                        } catch (Exception e) {
                                            //业务异常返回给客户端
                                            rpcInvocation.setE(e);
                                        }
                                    }
                                    break;
                                }

                            }
                            boolean isAsync = (boolean) rpcInvocation.getAttachments().get("async");
                            if (isAsync){
                                //如果是异步请求则不用返回结结果，减少网络传输
                                return;
                            }
                            rpcInvocation.setResponse(result);
                            //doAfterFilter 后置处理器
                            SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInvocation);
                            RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
                            serverChannelReadData.getChannelHandler().writeAndFlush(respRpcProtocol);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startDataConsume() {
        Thread thread = new Thread(new ServerJobCoreHandle());
        thread.start();
    }
}
