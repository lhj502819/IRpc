package cn.onenine.irpc.framework.core.common.exception;

import cn.onenine.irpc.framework.core.common.RpcInvocation;

/**
 * Description：
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 15:51
 */
public class MaxServiceLimitRequestException extends IRpcException{
    public MaxServiceLimitRequestException(String message,RpcInvocation rpcInvocation) {
        super(message,rpcInvocation);
    }
}
