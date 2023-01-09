# IRpc 手写RPC框架第7版-容错性相关设计
<a name="AqWaw"></a>
## 思考
RPC框架除了需要关注吞吐能力之外，对于失败场景的应对也是一个非常关键的点，本次我们主要从以下几个场景进行分析解决：

- 服务端异常返回给到调用方展示
- 客户端调用支持超时重试机制
- 服务提供方支持接口限流

<a name="ZmUZl"></a>
## 服务端异常信息
<a name="wssOZ"></a>
### 问题
目前我们的RPC框架中，当服务端发生异常时，异常信息并没有返回给客户端，这种情况带来的问题如下：

- 在分布式场景下无疑会增加我们的排查错误的成本，每次Client端调用异常时都需要找到对应Service Provider查询对应的异常信息；
- 如果Server端打印的异常不够丰富的话，无法判断是由那个Client发起的调用，更加增加了问题定位的困难；
- 服务端日志堆积严重。
  <a name="EfLlx"></a>
### 框架优化
我们的设计思路是**将服务端的异常信息返回给Client，并且将堆栈记录打印出来。**<br />我们之前会将请求的响应数据统一放到`RpcInvocation`中，这里我们将异常信息也放入该实体中。
```java
public class RpcInvocation implements Serializable {

    private static final long serialVersionUID = 4925694661803675105L;
    
    .........省略部分代码...............

    /**
     * 用于匹配请求和响应的一个关键值，当请求从客户端发出的时候，会有一个uuid用于记录发出的请求
     *  待数据返回的时候通过uuid来匹配对应的请求线程，并且返回给调用线程
     */
    private String uuid;

    /**
     * 接口响应的数据塞入这个字段中(如果是异步调用或者是void类型，这里就为空)
     */
    private Object response;

    /**
     * 异常堆栈
     */
    private Throwable e;

    .........省略部分代码...............
}
```
同时在执行目标方法发生异常时，将异常进行放入：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673272112163-d21f3925-a131-4fcb-94c4-90365a22ffd6.png#averageHue=%232d2b2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=864&id=ua574f9c4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=864&originWidth=1363&originalType=binary&ratio=1&rotation=0&showTitle=false&size=100725&status=done&style=none&taskId=u7f1af861-2552-4947-b3c7-db3564a8d30&title=&width=1363)<br />在Client接收到响应之后判断异常信息字段是否为空，如果为空则打印异常日志：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673272296910-6229bdf5-a791-42af-a79b-3803f154c1fa.png#averageHue=%232f2c2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=710&id=u04e8f2f8&margin=%5Bobject%20Object%5D&name=image.png&originHeight=710&originWidth=1125&originalType=binary&ratio=1&rotation=0&showTitle=false&size=88349&status=done&style=none&taskId=u5815cf12-3da0-405c-859a-f6d15d0c488&title=&width=1125)<br />**问题：**<br />由于异常堆栈的信息可能会非常多，TCP传输的数据体积过大，会导致一份数据包被拆解成多份进行传输。<br />因此在实际调用时Client端在进行数据读取的时候可能会报错：
```java
java.lang.IndexOutOfBoundsException: readerIndex(11) + length(11) exceeds writerIndex(11): PooledSlicedByteBuf(ridx: 11, widx: 11, cap: 11/11, unwrapped: PooledUnsafeDirectByteBuf(ridx: 64, widx: 64, cap: 1024))
        at io.netty.buffer.AbstractByteBuf.checkReadableBytes0(AbstractByteBuf.java:1403)
        at io.netty.buffer.AbstractByteBuf.checkReadableBytes(AbstractByteBuf.java:1390)
        at io.netty.buffer.AbstractByteBuf.readBytes(AbstractByteBuf.java:872)
```
我们通过采用netyy自带的协议封装规则来解决拆包粘包的问题，但是一旦遇到大体积的数据量还是会出现此类问题。<br />因此我们可以通过指定分隔符，并且通过参数定义每次传输的最大数据包体积，这样可以告知服务端每次读取的数据包的上限为配置的字节数长度，并且如果在这个分隔符内没有读取到完整的协议内容，则属于是异常数据包。**调整之后的代码如下：**
```java
public class RpcEncoder extends MessageToByteEncoder<RpcProtocol> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcProtocol msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagicNumber());
        out.writeInt(msg.getContentLength());
        out.writeBytes(msg.getContent());
        out.writeBytes(RpcConstants.DEFAULT_DECODE_CHAR.getBytes());
    }
    
}
```
```java
public class RpcDecoder extends ByteToMessageDecoder {

    /**
     * 协议的开头部分的标注长度
     */
    public final int BASE_LENGTH = 2 + 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= BASE_LENGTH) {
            //这里对应了RpcProtocol的魔数
            if (!(in.readShort() == MAGIC_NUMBER)) {
                ctx.close();
                return;
            }
            int length = in.readInt();
            //说明剩余的数据包不是完整的，这里需要重置下readerIndex
            if (in.readableBytes() < length) {
                ctx.close();
                return;
            }

            //这里其实就是实际的RpcProtocol对象的content字段
            byte[] data = new byte[length];
            in.readBytes(data);
            RpcProtocol rpcProtocol = new RpcProtocol(data);
            out.add(rpcProtocol);
        }
    }
}
```
在初始化Netty时分别假如对应的编码器：<br />Client：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673272910092-f795dcde-70ef-4403-9c6d-2cf05ddafca3.png#averageHue=%232f2c2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=504&id=u312ed058&margin=%5Bobject%20Object%5D&name=image.png&originHeight=504&originWidth=1329&originalType=binary&ratio=1&rotation=0&showTitle=false&size=85123&status=done&style=none&taskId=uda14c849-b927-4568-bf84-34630b2a465&title=&width=1329)<br />Server：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673272943518-b1b9a5f2-4823-4edd-8322-663a34e4c037.png#averageHue=%232f2c2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=716&id=uc1c817d4&margin=%5Bobject%20Object%5D&name=image.png&originHeight=716&originWidth=1055&originalType=binary&ratio=1&rotation=0&showTitle=false&size=114579&status=done&style=none&taskId=ud8d378eb-9d7e-4486-b347-a7a6a5098fc&title=&width=1055)
<a name="yQsiA"></a>
## 客户端调用支持超时重试机制
<a name="cGx5O"></a>
### 什么情况下适合进行超时重试？

- 当Service的两个Provider所在A、B两个机器的服务器性能不佳时，处理请求比较缓慢，B服务器的性能比A好，当调用A服务器发生超时的时候，可以尝试重新调用，将请求转到B机器上；
- 由于网络问题导致的请求超时，可以进行重试。
  <a name="ITsZz"></a>
### 什么情况不适合超时重试？
面对一些幂等性的接口调用，重试机制应该谨慎使用，比如：转账、下单以及一些金融相关的接口，当调用发生超时的时候，不好确认请求是否到达Server，如果重试的话可能会造成数据的错误，这种情况下的重试机制还是要谨慎的。
<a name="h9EnK"></a>
### 超时重试实现
Client调用时增加超时重试次数<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673273605392-4eee7be2-a97d-401a-bd6f-e1a18a0ed85b.png#averageHue=%232f2c2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=479&id=uc4e0933f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=479&originWidth=1021&originalType=binary&ratio=1&rotation=0&showTitle=false&size=78294&status=done&style=none&taskId=u2d7222f2-379e-4d86-beba-7939c35ab8c&title=&width=1021)<br />在动态代理逻辑中进行超时重试，具体代码如下：
```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArgs(args);
        rpcInvocation.setTargetMethod(method.getName());
        rpcInvocation.setTargetServiceName(rpcReferenceWrapper.getAimClass().getName());
        //这里面注入了一个uuid，对每一次的请求都单独区分
        rpcInvocation.setUuid(UUID.randomUUID().toString());
        rpcInvocation.setAttachments(rpcReferenceWrapper.getAttatchments());
        rpcInvocation.setRetry(rpcReferenceWrapper.getRetry());
        SEND_QUEUE.add(rpcInvocation);

        if (rpcReferenceWrapper.isAsync()) {
            return null;
        }
        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);

        long beginTime = System.currentTimeMillis();
        long nowTimeMillis = System.currentTimeMillis();

        //总重试次数
        int retryTimes = 0;
        //客户端请求超时的判断依据
        while (nowTimeMillis - beginTime < timeout || rpcInvocation.getRetry() > 0) {
            Object object = RESP_MAP.get(rpcInvocation.getUuid());
            if (object instanceof RpcInvocation) {
                RpcInvocation rpcInvocationResp = (RpcInvocation) object;
                if (rpcInvocation.getRetry() == 0 && rpcInvocationResp.getE() == null) {
                    return rpcInvocationResp.getResponse();
                } else if (rpcInvocation.getE() != null) {
                    //重试
                    if (rpcInvocation.getRetry() == 0) {
                        return rpcInvocationResp.getResponse();
                    }

                    //只有因为超时才会进行重试，否则重试不生效
                    if (nowTimeMillis - beginTime < timeout) {
                        retryTimes++;
                        //重新请求
                        rpcInvocation.clearRespAndError();
                        //每次重试的时候都将需重试次数减1
                        rpcInvocation.setRetry(rpcInvocationResp.getRetry() - 1);
                        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
                        SEND_QUEUE.add(rpcInvocation);
                    }
                }
            } else {
                nowTimeMillis = System.currentTimeMillis();
            }
        }
        RESP_MAP.remove(rpcInvocation.getUuid());
        throw new TimeoutException("Wait for response from server on client " + rpcReferenceWrapper.getTimeOUt() + "ms, retry times is " + retryTimes + "Server's name is " + rpcInvocation.getTargetServiceName() + "#" + rpcInvocation.getTargetMethod());
    }
```
<a name="GkbOs"></a>
### 重试的方式

- 间隔重试：适用于对于实时性没有要求的场景
- 立即重试：调用失败后立即进行重试，并且会路由到其他的机器上，在RPC的分布式场景下用的比较多，对服务的容错性有一定提升

我们也可以使用目前市面上比较流行的框架提供的重试机制，比如Goog Guava retry和Spring retry。
<a name="oi7mK"></a>
## 服务端接口限流
在微服务的场景下，通常Server端的请求会比Client端的大很多，所以为了防止由于请求激增把Server干掉，因此需要设置合理的流量阈值，适当的Server进行保护
<a name="JTw6H"></a>
### 保护点

- 整个Server端连接数的限制
- 单个Service的请求限流
- 方法级别限流
  <a name="ukcN7"></a>
### 整个Server端连接数限制
由于我们是基于Netty进行NIO通信，所有向Sever端发起的调用都需要建立一个连接，当Server端连接数达到某个上限的时候，则直接拒绝连接。<br />在Netty中，由于我们的Server端都会将`accept`单独由mainReactor负责，而workerReactor则负责IO请求，那我们的连接数记录和限制则可以在mainReactor中实现，我们可以通过自定义一个ChannelHandler，限制最大连接数，当连接数超过阈值后则立即关闭channel，通知Client。
```java
@ChannelHandler.Sharable
public class MaxConnectionLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxConnectionLimitHandler.class);

    private final int maxConnectionNum;

    private final AtomicInteger numConnection = new AtomicInteger(0);

    private final Set<Channel> childChannel = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final LongAdder numDroppedConnections = new LongAdder();

    private final AtomicBoolean loggingScheduled = new AtomicBoolean(false);


    public MaxConnectionLimitHandler(int maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = (Channel) msg;
        int conn = numConnection.incrementAndGet();
        if (conn > 0 && conn <= maxConnectionNum) {
            this.childChannel.add(channel);
            channel.closeFuture().addListener(future -> {
                childChannel.remove(channel);
                numConnection.decrementAndGet();
            });
            super.channelRead(ctx, msg);
        } else {
            numConnection.decrementAndGet();
            //立即关闭tcp连接
            channel.config().setOption(ChannelOption.SO_LINGER, 0);
            //立即关闭channel
            channel.unsafe().closeForcibly();
            numDroppedConnections.increment();
            if (loggingScheduled.compareAndSet(false,true)){
                //延时打印日志
                ctx.executor().schedule(this::writeNumDroppedConnectionLog,1, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 记录连接失败的日志
     */
    private void writeNumDroppedConnectionLog() {
        loggingScheduled.set(false);
        final long dropped = numDroppedConnections.sumThenReset();
        if(dropped>0){
            LOGGER.error("Dropped {} connection(s) to protect server,maxConnection is {}",dropped,maxConnectionNum);
        }
    }


}
```
<a name="XnIDd"></a>
### 单个Service的请求限流
实现方法：当接收到请求后，每次接收到请求后判断该Service的请求数是否达到阈值，达到阈值则直接返回错误，如果为达到阈值则将该Service计数+1，当目标方法执行完之后，将计数-1。<br />在Jdk中提供了`Semaphore`信号量并发工具类，具体的用法大家可以自行查询下，很简单，只需要在初始化时指定大小，每次调用`#acquire、#tryAcquire`等方法则会尝试将计数-1，如果计数为0则会返回，在执行完成后可通过执行`#release`来将计数归还，但每个API的具体行为有些不同，#acquire会阻塞等待至有信号量被释放，而`#tryAcquire`则会立即返回，这里我们使用后者，因为如果大量请求打入，会导致大量的线程阻塞，影响整个程序的正常运行。<br />**实现：**<br />之前我们在框架中增加了过滤器，这里我们也通过过滤器来实现，但我们需要对过滤器进行细化，分为前/后置过滤器，前置过滤器在方法执行前执行，起到将计数-1的目的，而后置过滤器则在方法执行后执行，负责将计数归还。
```java
@SPI("before")
public class ServerServiceBeforeLimitFilterImpl implements IServerFilter{

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServiceBeforeLimitFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult){
            String message = String.format("[ServerServiceBeforeLimitFilterImpl#doFilter] %s's max request is %s,reject now", serviceName, serverServiceSemaphoreWrapper.getMaxNums());
            LOGGER.error(message);
            MaxServiceLimitRequestException requestException = new MaxServiceLimitRequestException(message,rpcInvocation);
            rpcInvocation.setE(requestException);
            throw requestException;
        }
    }

}
```
```java
@SPI("after")
public class ServerServiceAfterLimitFilterImpl implements IServerFilter {
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName)
                .getSemaphore()
                .release();
    }
}
```
在进行初始化时也需要将前/后置过滤器分别加载保存：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1673275305420-702a64e4-54d5-4af1-85fe-db4ad164a33d.png#averageHue=%232f2c2b&clientId=ud2a7759e-040b-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=854&id=ub7564275&margin=%5Bobject%20Object%5D&name=image.png&originHeight=854&originWidth=1240&originalType=binary&ratio=1&rotation=0&showTitle=false&size=139470&status=done&style=none&taskId=u0b9d00e8-a57f-4e79-9ebc-c145724c82f&title=&width=1240)
<a name="Ru6dE"></a>
### 方法级别限流
方法级别限流其实也类似，我们就不实现了，小伙伴们感兴趣的话可以自行实现。
<a name="dgbZk"></a>
## 总结
本次我们主要针对RPC框架的容错性进行了优化，主要包含以下内容：

- 服务端异常日志的返回
- 客户端调用超时重试
- 服务端连接数限制
- 服务端Service请求数限流

其实在容错性方面还有很多待优化的空间，比如方法级别的限流、超时或者异常之后指定参数回调、服务降级、注册中心异常后的自动重连、Server支持动态调整限流参数等。
