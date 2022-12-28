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

    private Integer port;

    private Integer weight;

    public ChannelFutureWrapper() {
    }

    public ChannelFutureWrapper(String host, Integer port, Integer weight) {
        this.host = host;
        this.port = port;
        this.weight = weight;
    }

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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
