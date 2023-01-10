# IRpc **手写RPC框架v3-路由模块设计与实现**
> 源码地址：[https://github.com/lhj502819/IRpc/tree/v3](https://github.com/lhj502819/IRpc/tree/v3)

<a name="jrlHz"></a>

## 为什么需要路由模块？

在当今互联网日益发展的情况下，我们一个服务一般都会部署多个，一方面可以均摊压力，另一方面也可以增加容错性，提高我们系统的稳定性。<br />但这种情况无疑会提升系统的复杂性，这里我们只讨论在进行RPC远程调用的时候我们需要考虑的事情。如果只有一个服务提供者Provider的情况下，直接根据ip + port请求即可，如果有多个Provider的话，那么就需要一套合适的负载均衡算法去选择一个合适的Provider。<br />如果没有路由模块的话，我们也可以很简单的实现，比如在上一版本中我们通过jdk自带的`Random`函数进行的随机选择。<br />![image.png](https://cdn.nlark.com/yuque/0/2022/png/1171730/1672322226543-591ed835-7a71-444d-9f38-8a77ea91aa06.png#averageHue=%232e2b2a&clientId=ub2350b76-3c1a-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=269&id=u05042cbb&margin=%5Bobject%20Object%5D&name=image.png&originHeight=269&originWidth=1126&originalType=binary&ratio=1&rotation=0&showTitle=false&size=37330&status=done&style=none&taskId=u1443eaa5-a6c4-4d9e-995b-60ef919f157&title=&width=1126)<br />但这样做有以下几个弊端：

- 假设目标机器的性能不一致，如何对机器进行权重分配？
- 每次都要执行`Random`函数，在高并发情况下对CPU的消耗较高；
- 如何基于路由策略做ABTest？

因此我们单独抽象出一个模块来做这些工作，也就是**路由模块。**
<a name="VJ74b"></a>

## jdk Random随机函数的缺点

通过查看`Random`函数的源码我们就能知道，由于`Random`函数底层会调用`System.nanTome()`，此函数会发起一次系统调用，而系统调用就涉及到CPU的状态切换，对性能的消耗是极大的。**因此我们如果需要用到随机算法的话，最好自己实现一套。**<br />![image.png](https://cdn.nlark.com/yuque/0/2022/png/1171730/1672322376778-a45d0c5b-96be-4a77-b7ba-a865b92bcb00.png#averageHue=%232f2e2d&clientId=ub2350b76-3c1a-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=111&id=ufc2c0561&margin=%5Bobject%20Object%5D&name=image.png&originHeight=111&originWidth=557&originalType=binary&ratio=1&rotation=0&showTitle=false&size=9955&status=done&style=none&taskId=u0ca48cb9-e270-42df-aca2-8160070ddec&title=&width=557)
<a name="r26ls"></a>

## 路由抽象

```java
public interface IRouter {

    /**
     * 刷新路由数组
     * @param selector
     */
    void refreshRouterArr(Selector selector);

    /**
     * 获取对应provider的连接通道
     * @param selector
     * @return
     */
    ChannelFutureWrapper select(Selector selector);

    /**
     * 更新权重值
     */
    void updateWeight(URL url);

}
```

<a name="G83jW"></a>

## 负载均衡算法

<a name="aB594"></a>

### 随机算法

对应源代码中的`cn.onenine.irpc.framework.core.router.RandomRouterImpl`<br />实现思想：提前将所有的连接打乱顺序，随机放到数组中，也能达到随机访问的效果，但访问的顺序是不变的。**当Client连接完成后，则调用此方法打乱顺序。**

```java
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
```

<a name="cwZsc"></a>

### 权重算法

每个Provider在向注册中心注册的时候，都会设置自身的权重值为100，Client会在与Provider建立连接之后开启一个NodeData Watcher，当监听到Provider节点数据发生变化时，则会发起一个自定义的事件`IRpcNodeChangeEvent`，通知我们的路由策略进行权重刷新（`updateWeight`）。<br />![image.png](https://cdn.nlark.com/yuque/0/2022/png/1171730/1672323924765-41226685-861c-4b92-af83-37f80eb5cb38.png#averageHue=%23fdfdfc&clientId=ub2350b76-3c1a-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=805&id=u0ec4120a&margin=%5Bobject%20Object%5D&name=image.png&originHeight=805&originWidth=1694&originalType=binary&ratio=1&rotation=0&showTitle=false&size=44797&status=done&style=none&taskId=u0f9b989e-dc1f-4505-9174-205c80898e0&title=&width=1694)<br />如下为核心实现逻辑：

```java
@Override
public void updateWeight(URL url) {
    List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(url.getServiceName());
    //创建根据权重值创建对应的数组，权重大的其index在数组中占比大
	//比如channelFutureWrappers的第3个weight占比为50%，其他的4个总共占比50%
	//那么weightArr中则大概长这样：3,3,3,3,0,1,2,4
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
```

<a name="bxaQ1"></a>

### 轮询算法

通过自增计数，对数组长度取余的方式进行轮询访问。

```java
public class ChannelFuturePollingRef {

    private AtomicLong referenceTimes = new AtomicLong(0);

    /**
     * 对Providers实现轮询访问
     */
    public ChannelFutureWrapper getChannelFutureWrapper(String serviceName) {
        ChannelFutureWrapper[] wrappers = SERVICE_ROUTER_MAP.get(serviceName);
        //自增取余，顺序访问
        //0 % 10 = 0; 1 % 10 = 1; 2 % 10 = 2 ;....;11 % 10 = 1
        long i = referenceTimes.getAndIncrement();
        int index = (int) (i % wrappers.length);
        return wrappers[index];
    }

}
```

<a name="DytWV"></a>

### 其他路由算法

- **最小连接数**

需要记录每个应用服务器正在处理的连接数，然后将新来的请求转发到最少的那台上。

- **分布式哈希一致性算法**

分布式哈希一致性算法在实际使用时可能会出现“**哈希倾斜**”问题，为了解决这类问题，通常在算法的内部会设计一些虚拟节点，从而平衡请求的均匀性。

- **ip的hash算法**

通过将源地址通过hash计算，定位到具体的一台机器上，但是如果一旦某台机器崩溃的话，该IP的请求就会直接失败，容错性不强。
<a name="iJfuF"></a>

## 路由策略配置化

将具体的路由策略通过配置的方式，使用起来更加灵活。在Client初始化的时候，会根据不同的配置选择对应的路由策略实现。

```java
private void initConfig() {
    //初始化路由策略
    String routeStrategy = clientConfig.getRouteStrategy();
    if (RANDOM_ROUTER_TYPE.equals(routeStrategy)) {
        IROUTER = new RandomRouterImpl();
    } else if (ROTATE_ROUTER_TYPE.equals(routeStrategy)) {
        IROUTER = new RotateRouterImpl();
    }
}
```

<a name="w8AcS"></a>

## 总结

本次我们完成了RPC框架中路由层的设计与实现，并实现了随机路由算法、根据权重进行访问和轮询算法。