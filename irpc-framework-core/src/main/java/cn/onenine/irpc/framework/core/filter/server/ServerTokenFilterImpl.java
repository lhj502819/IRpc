package cn.onenine.irpc.framework.core.filter.server;

import cn.hutool.core.util.StrUtil;
import cn.onenine.irpc.framework.core.common.RpcInvocation;
import cn.onenine.irpc.framework.core.server.ServiceWrapper;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP;

/**
 * Description：服务端Token校验（简单实现）
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 22:00
 */
public class ServerTokenFilterImpl implements IServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String token = (String) rpcInvocation.getAttachments().get("token");
        ServiceWrapper serviceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInvocation.getTargetServiceName());
        if (serviceWrapper == null) {
            return;
        }
        String matchToken = serviceWrapper.getServiceToken();
        if (StrUtil.isBlank(matchToken)) {
            return;
        }
        if (StrUtil.isNotBlank(token) && matchToken.equals(token)) {
            return;
        }

        throw new RuntimeException("token is " + token + " verify result is false!");

    }
}
