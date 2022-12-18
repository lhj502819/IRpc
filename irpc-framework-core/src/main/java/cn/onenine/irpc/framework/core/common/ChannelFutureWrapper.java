package cn.onenine.irpc.framework.core.common;

import io.netty.channel.ChannelFuture;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 20:57
 */
public class ChannelFutureWrapper {

    private ChannelFuture channelFuture;

    private String host;

    private String port;

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public void setChannelFuture(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
