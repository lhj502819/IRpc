package cn.onenine.irpc.framework.core.filter.server;

import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.annotations.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description：服务端日志过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:57
 */
@SPI("before")
public class ServerLogFilterImpl implements IServerFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLogFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        LOGGER.info(rpcInvocation.getAttachments().get("c_app_name") + " do invoke ----->" + rpcInvocation.getTargetServiceName() + "#" +rpcInvocation.getTargetMethod());
    }
}
