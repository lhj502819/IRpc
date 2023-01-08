package cn.onenine.irpc.framework.core.client;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.RpcProtocol;
import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.CLIENT_SERIALIZE_FACTORY;
import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.RESP_MAP;

/**
 * 客户端接收服务端的响应数据
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:44
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    public static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] reqContent = rpcProtocol.getContent();
        RpcInvocation rpcInvocation = CLIENT_SERIALIZE_FACTORY.deserialize(reqContent, RpcInvocation.class);
        if (rpcInvocation.getE() != null) {
            rpcInvocation.getE().printStackTrace();
        }

        //通过之前发送的uuid来注入匹配的响应数值
        if (!RESP_MAP.containsKey(rpcInvocation.getUuid())){
            LOGGER.info("[ClientHandler#channelRead] request not found method:{} uuid: {} timeStamp:{}" ,rpcInvocation.getTargetMethod(), rpcInvocation.getUuid(),System.currentTimeMillis());
            LOGGER.info("[ClientHandler#channelRead]RESP_MAP all keys:{} timeStamp:{}",JSONObject.toJSONString(RESP_MAP.keySet()),System.currentTimeMillis());
//            throw new IllegalArgumentException("server response is error");
            Thread.sleep(500);
            if (!RESP_MAP.containsKey(rpcInvocation.getUuid())){
                LOGGER.info("sleep 500 millions after uuid also not found {}",rpcInvocation.getUuid());
            }else {
                LOGGER.info("sleep 500 millions after uuid  founded {}",rpcInvocation.getUuid());
            }
        }

        //将请求的响应结构放入一个Map集合中，集合的key就是uuid，这个uuid在发送请求之前就已经初始化好了
        //所以只需要起一个线程在后台遍历这个map，查看对应的key是否有响应即可
        //uuid放入map的操作被封装到了代理类中进行实现
        RESP_MAP.put(rpcInvocation.getUuid(),rpcInvocation);
        ReferenceCountUtil.release(msg);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
