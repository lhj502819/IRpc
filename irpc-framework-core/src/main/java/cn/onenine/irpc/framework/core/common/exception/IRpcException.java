package cn.onenine.irpc.framework.core.common.exception;

import cn.onenine.irpc.framework.core.common.RpcInvocation;

/**
 * Description：RPC框架异常基类
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/7 15:50
 */
public class IRpcException extends RuntimeException{

    private RpcInvocation rpcInvocation;

    private String message;

    public IRpcException(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }

    public IRpcException(String message, RpcInvocation rpcInvocation) {
        super(message);
        this.rpcInvocation = rpcInvocation;
    }

    public RpcInvocation getRpcInvocation() {
        return rpcInvocation;
    }

    public void setRpcInvocation(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }
}
