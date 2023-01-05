package cn.onenine.irpc.framework.consumer.route;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.registy.URL;
import cn.onenine.irpc.framework.core.router.IRouter;
import cn.onenine.irpc.framework.core.router.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.*;

/**
 * Description：轮询访问路由
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/28 22:46
 */
public class RotateRouterImplV2 implements IRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateRouterImplV2.class);

    @Override
    public void refreshRouterArr(Selector selector) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        for (int i = 0; i < channelFutureWrappers.size(); i++) {
            arr[i] = channelFutureWrappers.get(i);
        }

        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(),arr);
    }

    @Override
    public ChannelFutureWrapper select(Selector selector) {
        LOGGER.info("RotateRouterImplV2执行.......");
        return CHANNEL_FUTURE_POLLING_REF.getChannelFutureWrapper(selector.getChannelFutureWrappers());
    }

    @Override
    public void updateWeight(URL url) {

    }
}
