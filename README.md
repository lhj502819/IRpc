# IRpc **手写RPC框架v5-过滤器模块设计与实现**
<a name="Tr7YJ"></a>
## 为什么需要过滤器？
目前整个RPC框架的功能基本已经齐全了，但是在实际的开发过程中我们可能会有如下的需求：

- 对client的请求做鉴权
- 对服务进行分组管理
- 记录请求日志
- 基于IP的请求直连

首先我们先对这几个需求进行简单解释，只有弄清楚需求的来源才能更好的去理解、设计和实现。
<a name="bUKJ2"></a>
### 对client的请求做鉴权
随着业务的不断发展，服务的种类变得越来越丰富，有些重要的操作可能安全性比较高，需要进行鉴权，因此需要在RPC框架中增加对鉴权的支持。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672813394879-00c22166-58f2-4c69-9ee1-914e216bbc61.png#averageHue=%23e6f2ca&clientId=u500908ca-7289-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=324&id=u049f31ca&margin=%5Bobject%20Object%5D&name=image.png&originHeight=405&originWidth=1083&originalType=binary&ratio=1&rotation=0&showTitle=false&size=23186&status=done&style=none&taskId=u295a332f-bdd8-4864-b9f1-736228f9f1c&title=&width=866.4)
<a name="Dv4Ab"></a>
### 对服务进行分组管理
在进行团队协作或者服务升级的时候，可能会遇到需要对Service Provider进行分组，比如分为V1、V2，方便进行流量的划分，当V2版本出现问题时，我们只需要将所有的调用调整为V1即可，同时也可以进行迭代升级，以免出现ALL IN时升级的功能出现问题导致整个系统不可用。.<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672816758098-6c8cb8e3-26bf-4a99-8ebe-01a2ab25fc13.png#averageHue=%23e6f1ca&clientId=u500908ca-7289-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=329&id=ubc596961&margin=%5Bobject%20Object%5D&name=image.png&originHeight=411&originWidth=1157&originalType=binary&ratio=1&rotation=0&showTitle=false&size=45971&status=done&style=none&taskId=u29c8f5fc-2721-4a62-8916-e858c1b0446&title=&width=925.6)
<a name="nwRo1"></a>
### 基于IP的请求直连
在测试和联调阶段比较常见，例如在服务部署之后，发现两个provider对相同的服务，相同的参数，返回的结果却不同，此时就可以通过指定IP进行直连，方便问题定位。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672816841853-cce7fead-a64a-4dad-9a7a-fb82732d49b0.png#averageHue=%23e5f0c9&clientId=u500908ca-7289-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=299&id=u55f6c4f4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=374&originWidth=1202&originalType=binary&ratio=1&rotation=0&showTitle=false&size=53023&status=done&style=none&taskId=u1931aef2-4fc1-4cc5-b72d-224e8b61bcc&title=&width=961.6)
<a name="Ta6i5"></a>
### 记录请求日志
在实际的业务中我们在进行服务调用的时候需要做一些日志埋点，对调用信息进行记录，方便进行问题的排查

以上的这些处理其实就是一个链条，我们仅需要将这些功能按照顺序插入到整个链条中，并在适当的位置执行整个链条即可，而这些一个个的功能，则类似于我们常见的过滤器一样。
<a name="FG7cZ"></a>
## 如何实现？
过滤器的实现一般都会基于责任链设计模式去设计，在目前比较流行的API网关`SprngCloudGateway`中也有类似的实现。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672817458720-ec1d7631-94c5-4c31-8361-50026f5d5ae7.png#averageHue=%232e2d2c&clientId=u500908ca-7289-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=405&id=eIZ2w&margin=%5Bobject%20Object%5D&name=image.png&originHeight=506&originWidth=1033&originalType=binary&ratio=1&rotation=0&showTitle=false&size=66900&status=done&style=none&taskId=ufc9c77c5-86f3-4a56-9d4c-286d83abcd4&title=&width=826.4)<br />我曾经对SCG的2.2.6版本源码进行过解析，感兴趣的小伙伴可前往查看，地址：[https://www.yuque.com/lihongjian/gui608](https://www.yuque.com/lihongjian/gui608)<br />在我们的RPC框架中也采用类似的方式，只不过进行了简单的变形。我们首先定义了过滤器标记接口：`IFilter`
```java
public interface IFilter {
}

```
由于我们的过滤器分为Client和Server两端使用的，因此分别抽象出`IClinetFilter`和`IServerFilter`。
```java
public interface IServerFilter extends IFilter {

    void doFilter(RpcInvocation rpcInvocation);

}

public interface IClientFilter extends IFilter {

    void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation);

}
```
<a name="uVIxa"></a>
### 服务分组过滤器
在Client发起调用时，我们将**分组信息**存储到了`attachements`中，是一个Map结构。
```java
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
```
<a name="m4C5f"></a>
### IP直连过滤器
与“服务分组过滤器”一样，Client会在发起调用时将请求的ip存储到attachments中。
```java
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
```
<a name="WP4Bx"></a>
### Token校验过滤器
Client发起调用时会将token放入`attachments`中，Server端在过滤器中会拿到本次请求的token和内存中对应服务的token进行比对。
```java
public class ServerTokenFilterImpl implements IServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String token = (String) rpcInvocation.getAttachments().get("token");
        ServiceWrapper serviceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInvocation.getTargetServiceName());
        if (serviceWrapper == null) {
            return;
        }
        String matchToken = serviceWrapper.getServiceToken();
        if (StrUtil.isBlank(matchToken)) {
            return;
        }
        if (StrUtil.isNotBlank(token) && matchToken.equals(token)) {
            return;
        }

        throw new RuntimeException("token is " + token + " verify result is false!");

    }
}
```
<a name="Ma7EX"></a>
### 过滤器链FilterChain
过滤器链主要是负责将所有的过滤器按照一定顺序串起来。与过滤器类似，过滤器链也需要分为Client和Server端。
<a name="JXR7N"></a>
#### ServerFilterChain
```java
public class ServerFilterChain {

    private static List<IServerFilter> iServerFilters = new ArrayList<>();

    public void addServerFilter(IServerFilter serverFilter){
        iServerFilters.add(serverFilter);
    }

    public void doFilter(RpcInvocation rpcInvocation){
        for (IServerFilter iServerFilter : iServerFilters) {
            iServerFilter.doFilter(rpcInvocation);
        }
    }

}
```
<a name="Ns2Og"></a>
#### ClientFilterChain
```java
public class ClientFilterChain {

    private static List<IClientFilter> iClientFilters = new ArrayList<>();

    public void addServerFilter(IClientFilter clientFilter) {
        iClientFilters.add(clientFilter);
    }

    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        for (IClientFilter iClientFilter : iClientFilters) {
            iClientFilter.doFilter(src, rpcInvocation);
        }
    }

}
```

<a name="naqJl"></a>
## RPC框架接入
<a name="vaEsE"></a>
### Client端接入
由于像服务分组过滤器、IP直连过滤器都需要根据指定的规则选择出对应的Provider，因此我们将执行过滤链条的逻辑插入在`cn.onenine.irpc.framework.core.client.ConnectionHandler#getChannelFuture`中。
```java
/**
 * 默认走随机策略获取ChannelFuture
 */
public static ChannelFuture getChannelFuture(RpcInvocation rpcInvocation) {
    ChannelFutureWrapper[] channelFutureWrappers = SERVICE_ROUTER_MAP.get(rpcInvocation.getTargetServiceName());
    if (channelFutureWrappers == null || channelFutureWrappers.length == 0) {
        throw new RuntimeException("no provider exist for " + rpcInvocation.getTargetServiceName());
    }
    //doFilter
    CLIENT_FILTER_CHAIN.doFilter(Lists.newArrayList(channelFutureWrappers), rpcInvocation);
    Selector selector = new Selector();
    selector.setProviderServiceName(rpcInvocation.getTargetServiceName());
    selector.setChannelFutureWrappers(channelFutureWrappers);
    //通过指定的路由算法选择一个Provider ChannelFuture
    return IROUTER.select(selector).getChannelFuture();
}
```
<a name="uaCut"></a>
### Server端接入
在初始化时按照指定顺序将所有的Filter插入到FilterChain中，在接收到请求后执行整个链条即可，也就是`ChannelInboundHandlerAdapter#channelRead`。
```java
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //服务端接收数据的时候以RpcProtocol协议的格式接收
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(),RpcInvocation.class);

        //doFilter
        SERVER_FILTER_CHAIN.doFilter(rpcInvocation);
        
    	//省略部分代码......
        rpcInvocation.setResponse(result);
        RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
        ctx.writeAndFlush(respRpcProtocol);
    }

}
```
<a name="h006L"></a>
## 总结
本版本我们基于责任链模式完成了对RPC框架中流程化功能的整合，这些零零散散的功能我们通过过滤器的方式进行了实现，比如服务的分组选择、IP直连、Token统一校验等，减少了各个模块间的耦合性，如果需要补充新的过滤器，只需要实现Client或者Server对应的接口即可。



