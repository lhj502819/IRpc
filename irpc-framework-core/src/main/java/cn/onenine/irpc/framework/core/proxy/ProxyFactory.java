package cn.onenine.irpc.framework.core.proxy;

import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:54
 */
public interface ProxyFactory {

    <T> T getProxy(final RpcReferenceWrapper<T> rpcReferenceWrapper) throws Throwable;

}
