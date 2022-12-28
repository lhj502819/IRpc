package cn.onenine.irpc.framework.core.router;

import cn.onenine.irpc.framework.core.common.ChannelFutureWrapper;
import cn.onenine.irpc.framework.core.registy.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cn.onenine.irpc.framework.core.common.cache.CommonClientCache.*;

/**
 * Description：随机路由算法
 * 为什么 jdk已经提供了Random函数，我们还要自定义呢？ 因为jdk的Random函数比较消耗性能：
 * JDK内部的Random类的底层设计基本思路是： 先通过一个初始化种子的函数，然后和181783497276652981这个数字做乘法，
 * 再将其与System.nanoTime()做与运算得倒一个随机的种子。注意 System.nanoTime()) 是一个本地方法，调用本地方法的时候需要涉及到系统的上下文切换会比较消耗性能。
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/27 22:17
 */
public class RandomRouterImpl implements IRouter {

    @Override
    public void refreshRouterArr(Selector selector) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(selector.getProviderServiceName());
        ChannelFutureWrapper[] arr = new ChannelFutureWrapper[channelFutureWrappers.size()];
        //提权生成调用先后顺序的随机数组
        int[] result = createRandomIndex(arr.length);
        //按照随机数组中的数字顺序，将所有的provider channel放入新的Channel数组中
        for (int i = 0; i < result.length; i++) {
            arr[i] = channelFutureWrappers.get(result[i]);
        }
        SERVICE_ROUTER_MAP.put(selector.getProviderServiceName(), arr);
    }

    @Override
    public ChannelFutureWrapper select(Selector selector) {
        return CHANNEL_FUTURE_POLLING_REF.getChannelFutureWrapper(selector.getProviderServiceName());
    }

    /**
     * 将ChannelFuture按照weight进行分配并乱序
     * @param url
     */
    @Override
    public void updateWeight(URL url) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(url.getServiceName());
        //创建根据权重值创建对应的数组，权重大的其index在数组中占比大
        Integer[] weightArr = createWeightArr(channelFutureWrappers);
        Integer[] randomArr = createRandomArr(weightArr);
        ChannelFutureWrapper[] finalChannelFutureWrappers = new ChannelFutureWrapper[randomArr.length];
        for (int i = 0; i < randomArr.length; i++) {
            finalChannelFutureWrappers[i] = channelFutureWrappers.get(randomArr[i]);
        }
        SERVICE_ROUTER_MAP.put(url.getServiceName(),finalChannelFutureWrappers);
    }

    public static Integer[] createWeightArr(List<ChannelFutureWrapper> channelFutureWrappers) {
        List<Integer> weightArr = new ArrayList<>();
        for (int k = 0; k < channelFutureWrappers.size(); k++) {
            Integer weight = channelFutureWrappers.get(k).getWeight();
            int c = weight / 100;
            for (int i = 0; i < c; i++) {
                weightArr.add(k);
            }
        }
        Integer[] arr = new Integer[weightArr.size()];
        return weightArr.toArray(arr);
    }

    /**
     * 创建随机乱序数组
     */
    public static Integer[] createRandomArr(Integer[] arr) {
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

    public static void main(String[] args) {
        RandomRouterImpl randomRouter = new RandomRouterImpl();
        int[] randomIndex = randomRouter.createRandomIndex(10);
        System.out.println(randomIndex);
    }

    private int[] createRandomIndex(int len) {
        int[] arrInt = new int[len];
        Random ra = new Random();
        for (int i = 0; i < arrInt.length; i++) {
            arrInt[i] = -1;
        }
        int index = 0;
        while (index < arrInt.length) {
            //随机生成length范围内数字
            int num = ra.nextInt(len);
            //如果数组中不包含这个元素则添加到数组中
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
