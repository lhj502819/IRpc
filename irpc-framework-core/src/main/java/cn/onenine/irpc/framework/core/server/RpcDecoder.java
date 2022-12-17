package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.core.common.RpcProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static cn.onenine.irpc.framework.core.common.constant.RpcConstants.MAGIC_NUMBER;

/**
 * Description：解码器
 *  在实现过程中需要考虑是否会有粘包拆包的问题，并且还要设置请求数据包体积最大值
 *  处理粘包拆包问题方案见{@see https://mp.weixin.qq.com/s/oN-gBB8eYn4rJH82YlD5Rw}
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 11:05
 */
public class RpcDecoder extends ByteToMessageDecoder {

    /**
     * 协议的开头部分的标注长度
     */
    public final int BASE_LENGTH = 2 + 4;

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= BASE_LENGTH){
            if (in.readableBytes() > 1000){
                //防止收到一些体积过大的数据包
                in.skipBytes(in.readableBytes());
            }
            int beginReader;
            while (true){
                //第一次为0
                beginReader = in.readerIndex();
                //标记readerIndex，可以通过resetReaderIndex，重新将buffer的readerIndex重置为标记的readerIndex
                in.markReaderIndex();
                //这里对应了RpcProtocol的魔数
                if (in.readShort() == MAGIC_NUMBER){
                    break;
                }else {
                    //如果不是魔数开头，说明是非法的客户端发来的数据包
                    ctx.close();
                    return;
                }
            }

            //这里对应RpcProtocol的contentLength字段
            int contentLength = in.readInt();
            //说明剩余的数据包不是完整的，这里需要重置下readerIndex
            if (in.readableBytes() < contentLength){
                in.readerIndex(beginReader);
                return;
            }

            //这里其实就是实际的RpcProtocol对象的content字段
            byte[] data = new byte[contentLength];
            in.readBytes(data);
            RpcProtocol rpcProtocol = new RpcProtocol(data);
            out.add(rpcProtocol);
        }
    }
}
