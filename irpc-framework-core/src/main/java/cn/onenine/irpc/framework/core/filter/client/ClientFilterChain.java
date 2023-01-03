package cn.onenine.irpc.framework.core.filter.client;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.filter.server.IServerFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Description：客户端过滤器链
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:23
 */
public class ClientFilterChain {

    private static List<IClientFilter> iClientFilters = new ArrayList<>();

    public void addServerFilter(IClientFilter clientFilter) {
        iClientFilters.add(clientFilter);
    }

    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        for (IClientFilter iClientFilter : iClientFilters) {
            iClientFilter.doFilter(src, rpcInvocation);
        }
    }

}