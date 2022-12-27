package cn.onenine.irpc.framework.core.router;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.registy.URL;

/**
 * Description：路由层的抽象
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/27 22:07
 */
public interface IRouter {

    /**
     * 刷新路由数组
     * @param selector
     */
    void refreshRouterArr(Selector selector);

    /**
     * 获取对应service的连接通道
     * @param selector
     * @return
     */
    ChannelFutureWrapper select(Selector selector);

    /**
     * 更新权重值
     */
    void updateWeight(URL url);

}
