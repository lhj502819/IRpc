package cn.onenine.irpc.framework.core.common;

import java.util.concurrent.atomic.AtomicLong;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.SERVICE_ROUTER_MAP;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/28 21:17
 */
public class ChannelFuturePollingRef {

    private AtomicLong referenceTimes = new AtomicLong(0);

    /**
     * 对Providers实现轮询访问
     */
    public ChannelFutureWrapper getChannelFutureWrapper(String serviceName) {
        ChannelFutureWrapper[] wrappers = SERVICE_ROUTER_MAP.get(serviceName);
        //自增取余，顺序访问
        //0 % 10 = 0; 1 % 10 = 1; 2 % 10 = 2 ;....;11 % 10 = 1
        long i = referenceTimes.getAndIncrement();
        int index = (int) (i % wrappers.length);
        return wrappers[index];
    }

}
