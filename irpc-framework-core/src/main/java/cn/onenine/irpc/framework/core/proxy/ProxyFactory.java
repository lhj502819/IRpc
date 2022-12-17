package cn.onenine.irpc.framework.core.proxy;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:54
 */
public interface ProxyFactory {

    <T> T getProxy(final  Class clazz) throws Throwable;

}
