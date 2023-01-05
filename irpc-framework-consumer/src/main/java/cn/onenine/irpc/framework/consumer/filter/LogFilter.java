package cn.onenine.irpc.framework.consumer.filter;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.filter.client.IClientFilter;

import java.util.List;

/**
 * Description：自定义日志过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/5 21:55
 */
public class LogFilter implements IClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        System.out.println("自定义日志过滤器");
    }
}
