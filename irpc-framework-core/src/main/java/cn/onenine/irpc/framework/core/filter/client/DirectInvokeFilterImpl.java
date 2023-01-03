package cn.onenine.irpc.framework.core.filter.client;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;

import java.util.Iterator;
import java.util.List;

/**
 * Description：直连过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:52
 */
public class DirectInvokeFilterImpl implements IClientFilter {
    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        String url = (String) rpcInvocation.getAttachments().get("url");
        if (StrUtil.isBlank(url)) {
            return;
        }

        Iterator<ChannelFutureWrapper> iterator = src.iterator();
        while (iterator.hasNext()) {
            ChannelFutureWrapper channelFutureWrapper = iterator.next();
            if (!(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort()).equals(url)) {
                iterator.remove();
            }
            if (CollectionUtil.isEmpty(src)) {
                throw new RuntimeException("no match for url:" + url);
            }
        }
    }
}
