package cn.onenine.irpc.framework.core.filter.client;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.CLIENT_CONFIG;

/**
 * Description：调用日志过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:30
 */
public class ClientLogFilterImpl implements IClientFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLogFilterImpl.class);

    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        Map<String, Object> attachments = rpcInvocation.getAttachments();
        attachments.put("c_app_name", CLIENT_CONFIG.getApplicationName());
        LOGGER.info(rpcInvocation.getAttachments().get("c_app_name")+" do invoke -----> "+rpcInvocation.getTargetServiceName());
    }
}
