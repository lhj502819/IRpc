package cn.onenine.irpc.framework.core.filter.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.filter.IFilter;

/**
 * Description：服务端过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:21
 */
public interface IServerFilter extends IFilter {

    void doFilter(RpcInvocation rpcInvocation);

}
