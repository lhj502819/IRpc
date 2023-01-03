package cn.onenine.irpc.framework.core.proxy.jdk;

import cn.onenine.irpc.framework.core.client.RpcReferenceWrapper;
import cn.onenine.irpc.framework.core.common.config.PropertiesBootstrap;
import cn.onenine.irpc.framework.core.proxy.ProxyFactory;

import java.lang.reflect.Proxy;

/**
 * Description：辅助客户端发起调用的代理对象
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:54
 */
public class JDKProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(RpcReferenceWrapper<T> rpcReferenceWrapper) throws Throwable {
        return (T) Proxy.newProxyInstance(rpcReferenceWrapper.getAimClass().getClassLoader(),
                new Class[]{rpcReferenceWrapper.getAimClass()}, new JDKClientInvocationHandler(rpcReferenceWrapper, PropertiesBootstrap.loadClientConfigFromLocal().getCallTimeout()));
    }
}
