package cn.onenine.irpc.framework.core.registy.zookeeper;

import cn.onenine.irpc.framework.core.common.cache.CommonClientCache;
import cn.onenine.irpc.framework.core.common.cache.CommonServerCache;
import cn.onenine.irpc.framework.core.registy.RegistryService;
import cn.onenine.irpc.framework.core.registy.URL;

import java.util.List;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.SUBSCRIBE_SERVICE_LIST;
import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_URL_SET;

/**
 * Description：主要作用是对一些注册数据做统一的处理，假设日后需要考虑支持多种类型的注册中心
 *  例如：Redis、etcd，所有基础的操作都可以统一放在抽象类里实现
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 20:31
 */
public abstract class AbstractRegister implements RegistryService {

    @Override
    public void register(URL url) {
        PROVIDER_URL_SET.add(url);
    }

    @Override
    public void unRegister(URL url) {
        PROVIDER_URL_SET.remove(url);
    }

    @Override
    public void subscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.add(url);
    }

    @Override
    public void doUnSubscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.remove(url);
    }

    /**
     * 留给子类扩展
     * @param url
     */
    public abstract void doAfterSubscribe(URL url);

    /**
     * 留给子类扩展
     */
    public abstract List<String> getProviderIps(String serviceName);

}
