package cn.onenine.irpc.framework.core.filter.client;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.filter.IFilter;

import java.util.List;

/**
 * Description：客户端过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:21
 */
public interface IClientFilter extends IFilter {

    /**
     * 执行过滤链
     * @param src
     * @param rpcInvocation
     */
    void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation);

}
