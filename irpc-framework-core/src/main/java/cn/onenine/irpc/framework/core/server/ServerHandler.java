package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.RpcProtocol;
import cn.onenine.irpc.framework.core.common.cache.CommonServerCache;
import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_CLASS_MAP;
import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.SERVER_SERIALIZE_FACTORY;

/**
 * Description：当数据抵达Handler的时候，已经是被解码过的了，也就是RpcProtocol
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 11:23
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //服务端接收数据的时候以RpcProtocol协议的格式接收
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(),RpcInvocation.class);
        //这里的PROVIDER_CLASS_MAP就是一开始预先在启动的时候存储的Bean集合
        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
        Method[] methods = aimObject.getClass().getMethods();
        Object result = null;
        for (Method method : methods) {
            if (method.getName().equals(rpcInvocation.getTargetMethod())){
                if (method.getReturnType().equals(Void.TYPE)){
                    method.invoke(aimObject,rpcInvocation.getArgs());
                }else {
                    result = method.invoke(aimObject,rpcInvocation.getArgs());
                }
                break;
            }
        }
        rpcInvocation.setResponse(result);
        RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
        ctx.writeAndFlush(respRpcProtocol);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();;
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
