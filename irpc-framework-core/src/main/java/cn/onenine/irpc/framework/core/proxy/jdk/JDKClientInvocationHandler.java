package cn.onenine.irpc.framework.core.proxy.jdk;

import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.config.ClientConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
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

    private final static Object OBJECT = new Object();

    private RpcReferenceWrapper rpcReferenceWrapper;

    private Long timeout ;


    public JDKClientInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper, Long executeTimeout) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
        if (executeTimeout == null){
            throw new IllegalArgumentException("executeTimeout must not null and gt 0 ");
        }
        this.timeout = executeTimeout;
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
        RESP_MAP.put(rpcInvocation.getUuid(),OBJECT);
        SEND_QUEUE.add(rpcInvocation);
        long beginTime = System.currentTimeMillis();
        //客户端请求超时的判断依据
        while (System.currentTimeMillis() - beginTime < timeout){
            Object object = RESP_MAP.get(rpcInvocation.getUuid());
            if (object instanceof  RpcInvocation){
                return ((RpcInvocation)object).getResponse();
            }
        }

        throw new TimeoutException("client wait server's response timeout!");
    }
}
