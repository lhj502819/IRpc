package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.core.common.RpcProtocol;
import cn.onenine.irpc.framework.core.dispatcher.ServerChannelReadData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.*;

/**
 * Description：当数据抵达Handler的时候，已经是被解码过的了，也就是RpcProtocol
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 11:23
 */
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
        serverChannelReadData.setChannelHandler(ctx);
        serverChannelReadData.setRpcProtocol((RpcProtocol) msg);
        //放入分发器
        SERVER_CHANNEL_DISPATCHER.add(serverChannelReadData);
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
