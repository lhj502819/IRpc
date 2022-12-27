package cn.onenine.irpc.framework.core.router;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.registy.URL;

import java.util.List;
import java.util.Random;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.CONNECT_MAP;
import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.SERVICE_ROUTER_MAP;

/**
 * Description：随即路由算法
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/27 22:17
 */
public class RandomRouterImpl implements IRouter{
    @Override
    public void refreshRouterArr(Selector selector) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        //提权生成段勇先后顺序的随机数组
        int[] result = createRandomIndex(arr.length);
        //生成对应服务集群的每台机器的调用顺序
        for (int i = 0; i < result.length; i++) {
            arr[i] = channelFutureWrappers.get(result[i]);
        }
        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(),arr);
    }

    @Override
    public ChannelFutureWrapper select(Selector selector) {
        return null;
    }

    @Override
    public void updateWeight(URL url) {

    }

    /**
     * 创建随机乱序数组
     */
    private static Integer[] createRandomArr(Integer[] arr){
        int total = arr.length;
        Random ra = new Random();
        for (int i = 0; i < total; i++) {
            int j = ra.nextInt(total);
            if (i == j) {
                continue;
            }
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;

        }
        return arr;
    }

    private int[] createRandomIndex(int len){
        int[] arrInt = new int[len];
        Random ra = new Random();
        for (int i = 0; i < arrInt.length; i++) {
            arrInt[i] = -1;
        }
        int index = 0;
        while (index < arrInt.length){
            int num = ra.nextInt(len);
            //如果数组中不包含这个元素则赋值给数组
            if (!contains(arrInt, num)) {
                arrInt[index++] = num;
            }
        }
        return arrInt;
    }

    public boolean contains(int[] arr, int key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == key) {
                return true;
            }
        }
        return false;
    }
}
