package cn.onenine.irpc.framework.core.client;

import cn.onenine.irpc.framework.core.proxy.ProxyFactory;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 14:05
 */
public class RpcReference {

    public ProxyFactory proxyFactory;

    public RpcReference(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * 根据接口类型获取代理对象
     */
    public <T> T get(RpcReferenceWrapper<T> rpcReferenceWrapper) throws Throwable{
        return proxyFactory.getProxy(rpcReferenceWrapper);
    }
}
