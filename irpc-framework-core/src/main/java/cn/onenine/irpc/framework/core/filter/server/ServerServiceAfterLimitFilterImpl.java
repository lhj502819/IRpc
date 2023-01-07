package cn.onenine.irpc.framework.core.filter.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.ServerServiceSemaphoreWrapper;
import cn.onenine.irpc.framework.core.common.annotations.SPI;
import cn.onenine.irpc.framework.core.common.cache.CommonServerCache;

/**
 * Description：服务方法限流过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 15:41
 */
@SPI("after")
public class ServerServiceAfterLimitFilterImpl implements IServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName)
                .getSemaphore()
                .release();
    }
}
