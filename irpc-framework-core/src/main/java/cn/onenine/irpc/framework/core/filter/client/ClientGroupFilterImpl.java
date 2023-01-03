package cn.onenine.irpc.framework.core.filter.client;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.common.RpcInvocation;

import java.util.Iterator;
import java.util.List;

/**
 * Description：基于Provider分组过滤器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 21:38
 */
public class ClientGroupFilterImpl implements IClientFilter{

    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        String group = (String) rpcInvocation.getAttachments().get("group");
        if (StrUtil.isBlank(group)){
            return;
        }
        Iterator<ChannelFutureWrapper> iterator = src.iterator();
        while (iterator.hasNext()) {
            ChannelFutureWrapper channelFutureWrapper = iterator.next();
            if (!channelFutureWrapper.getGroup().equals(group)){
                iterator.remove();
            }
        }
        if (CollectionUtil.isEmpty(src)){
            throw new RuntimeException("no provider match for group " + group);
        }

    }
}
