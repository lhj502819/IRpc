package cn.onenine.irpc.framework.core.common.event.listener;

import cn.onenine.irpc.framework.core.common.event.IRpcDestroyEvent;
import cn.onenine.irpc.framework.core.registy.URL;

import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_URL_SET;
import static cn.onenine.irpc.framework.core.common.cache.CommonServerCache.REGISTRY_SERVICE;

/**
 * Description：服务销毁事件监听者
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/29 17:19
 */
public class ServiceDestroyListener implements IRpcListener<IRpcDestroyEvent> {
    @Override
    public void callBack(Object t) {
        for (URL url : PROVIDER_URL_SET) {
            REGISTRY_SERVICE.unRegister(url);
        }
    }
}
