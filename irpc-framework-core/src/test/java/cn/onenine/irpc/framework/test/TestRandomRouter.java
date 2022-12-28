package cn.onenine.irpc.framework.test;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.router.RandomRouterImpl;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/28 22:34
 */
public class TestRandomRouter {

    @Test
    public void testCreateWeightArr(){
        ChannelFutureWrapper channelFutureWrapper1 = new ChannelFutureWrapper(null,null,150);
        ChannelFutureWrapper channelFutureWrapper2 = new ChannelFutureWrapper(null,null,250);
        ChannelFutureWrapper channelFutureWrapper3 = new ChannelFutureWrapper(null,null,1000);
        ArrayList<ChannelFutureWrapper> channelFutureWrappers = Lists.newArrayList(channelFutureWrapper1, channelFutureWrapper2, channelFutureWrapper3);
        Integer[] weightArr = RandomRouterImpl.createWeightArr(channelFutureWrappers);
        System.out.println(weightArr);
    }
    @Test
    public void testCreateRandomArr(){
        ChannelFutureWrapper channelFutureWrapper1 = new ChannelFutureWrapper(null,null,150);
        ChannelFutureWrapper channelFutureWrapper2 = new ChannelFutureWrapper(null,null,250);
        ChannelFutureWrapper channelFutureWrapper3 = new ChannelFutureWrapper(null,null,1000);
        ArrayList<ChannelFutureWrapper> channelFutureWrappers = Lists.newArrayList(channelFutureWrapper1, channelFutureWrapper2, channelFutureWrapper3);
        Integer[] weightArr = RandomRouterImpl.createWeightArr(channelFutureWrappers);
        Integer[] randomArr = RandomRouterImpl.createRandomArr(weightArr);
        System.out.println(randomArr);
    }

}
