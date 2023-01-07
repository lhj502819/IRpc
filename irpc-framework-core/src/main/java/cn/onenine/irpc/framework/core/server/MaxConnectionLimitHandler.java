package cn.onenine.irpc.framework.core.server;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Description：服务端连接数处理器，作用于mainReactor
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 15:13
 */
@ChannelHandler.Sharable
public class MaxConnectionLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxConnectionLimitHandler.class);

    private final int maxConnectionNum;

    private final AtomicInteger numConnection = new AtomicInteger(0);

    private final Set<Channel> childChannel = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final LongAdder numDroppedConnections = new LongAdder();

    private final AtomicBoolean loggingScheduled = new AtomicBoolean(false);


    public MaxConnectionLimitHandler(int maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = (Channel) msg;
        int conn = numConnection.incrementAndGet();
        if (conn > 0 && conn <= maxConnectionNum) {
            this.childChannel.add(channel);
            channel.closeFuture().addListener(future -> {
                childChannel.remove(channel);
                numConnection.decrementAndGet();
            });
            super.channelRead(ctx, msg);
        } else {
            numConnection.decrementAndGet();
            //立即关闭tcp连接
            channel.config().setOption(ChannelOption.SO_LINGER, 0);
            //立即关闭channel
            channel.unsafe().closeForcibly();
            numDroppedConnections.increment();
            if (loggingScheduled.compareAndSet(false,true)){
                //延时打印日志
                ctx.executor().schedule(this::writeNumDroppedConnectionLog,1, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 记录连接失败的日志
     */
    private void writeNumDroppedConnectionLog() {
        loggingScheduled.set(false);
        final long dropped = numDroppedConnections.sumThenReset();
        if(dropped>0){
            LOGGER.error("Dropped {} connection(s) to protect server,maxConnection is {}",dropped,maxConnectionNum);
        }
    }


}
