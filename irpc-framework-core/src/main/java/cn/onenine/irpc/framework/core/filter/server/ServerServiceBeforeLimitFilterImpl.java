package cn.onenine.irpc.framework.core.filter.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.ServerServiceSemaphoreWrapper;
import cn.onenine.irpc.framework.core.common.annotations.SPI;
import cn.onenine.irpc.framework.core.common.cache.CommonServerCache;
import cn.onenine.irpc.framework.core.common.exception.MaxServiceLimitRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

/**
 * Description：
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 15:47
 */
@SPI("before")
public class ServerServiceBeforeLimitFilterImpl implements IServerFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServiceBeforeLimitFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult){
            String message = String.format("[ServerServiceBeforeLimitFilterImpl#doFilter] %s's max request is %s,reject now", serviceName, serverServiceSemaphoreWrapper.getMaxNums());
            LOGGER.error(message);
            MaxServiceLimitRequestException requestException = new MaxServiceLimitRequestException(message,rpcInvocation);
            rpcInvocation.setE(requestException);
            throw requestException;
        }
    }

}
