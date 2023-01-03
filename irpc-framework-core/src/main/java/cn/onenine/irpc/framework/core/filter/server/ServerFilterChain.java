package cn.onenine.irpc.framework.core.filter.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Description：服务端过滤器链
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:23
 */
public class ServerFilterChain {

    private static List<IServerFilter> iServerFilters = new ArrayList<>();

    public void addServerFilter(IServerFilter serverFilter){
        iServerFilters.add(serverFilter);
    }

    public void doFilter(RpcInvocation rpcInvocation){
        for (IServerFilter iServerFilter : iServerFilters) {
            iServerFilter.doFilter(rpcInvocation);
        }
    }

}
