package cn.onenine.irpc.framework.core.proxy.jdk;

import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.common.exception.IRpcException;
import cn.onenine.irpc.framework.core.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.RESP_MAP;
import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.SEND_QUEUE;

/**
 * 代理处理器，核心任务就是将需要调用的方法名称、服务名称、参数都统统封装好到RpcInvocation当中
 * 然后塞入到一个队列中，并且等待服务端的数据返回
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:56
 */
public class JDKClientInvocationHandler implements InvocationHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(JDKClientInvocationHandler.class);

    private final static Object OBJECT = new Object();

    private RpcReferenceWrapper rpcReferenceWrapper;

    private Long timeout;


    public JDKClientInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
        this.timeout = Long.valueOf(rpcReferenceWrapper.getTimeOUt());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArgs(args);
        rpcInvocation.setTargetMethod(method.getName());
        rpcInvocation.setTargetServiceName(rpcReferenceWrapper.getAimClass().getName());
        //这里面注入了一个uuid，对每一次的请求都单独区分
        rpcInvocation.setUuid(UUID.randomUUID().toString());
        rpcInvocation.setAttachments(rpcReferenceWrapper.getAttatchments());
        rpcInvocation.setRetry(rpcReferenceWrapper.getRetry());
        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
        SEND_QUEUE.add(rpcInvocation);
        if (rpcReferenceWrapper.isAsync()) {
            return null;
        }
        long beginTime = System.currentTimeMillis();
        long nowTimeMillis = System.currentTimeMillis();

        //总重试次数
        int retryTimes = 0;
        try {
            //客户端请求超时的判断依据
            while (true) {
                Object object = RESP_MAP.get(rpcInvocation.getUuid());
                if (object instanceof RpcInvocation) {
                    RpcInvocation rpcInvocationResp = (RpcInvocation) object;
                    if (rpcInvocationResp.getE() == null) {
                        return rpcInvocationResp.getResponse();
                    } else if (rpcInvocationResp.getE() != null) {
                        if (rpcInvocation.getRetry() == 0) {
                            if (retryTimes > 0) {
                                throw new TimeoutException("Wait for response from server on client " + rpcReferenceWrapper.getTimeOUt() + "ms, retry times is " + retryTimes + "Server's name is " + rpcInvocation.getTargetServiceName() + "#" + rpcInvocation.getTargetMethod());
                            }
                            return rpcInvocationResp.getE().getMessage();
                        }

                        //只有因为超时才会进行重试，否则重试不生效
                        if (nowTimeMillis - beginTime > timeout) {
                            retryTimes++;
                            //重新请求
                            rpcInvocation.clearRespAndError();
                            //每次重试的时候都将需重试次数减1
                            rpcInvocation.setRetry(rpcInvocationResp.getRetry() - 1);
                            RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
                            SEND_QUEUE.add(rpcInvocation);
                        } else {
                            throw rpcInvocationResp.getE();
                        }

                    }
                } else {
                    nowTimeMillis = System.currentTimeMillis();
                }
            }
        } finally {
            LOGGER.info("[JDKClientInvocationHandler#invoke] irpc remove uuid {} timeStamp:{}" , rpcInvocation.getUuid(),System.currentTimeMillis());
            RESP_MAP.remove(rpcInvocation.getUuid());
        }
    }
}
