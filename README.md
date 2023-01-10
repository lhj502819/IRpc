# IRpc 手写RPC框架v7-基于线程和队列提升框架并发处理能力
<a name="yl3QV"></a>
## Server端
<a name="PrlhT"></a>
### 现有的问题
目前我们的RPC框架Server端在接收到请求之后会直接在Netty的IO线程中执行业务逻辑，如果业务逻辑比较简单还好，但是如果业务逻辑比较复杂，需要处理的时间又比较长，那就会对Netty的IO线程占用时间较长，导致阻塞住其他的请求，因此我们可能就需要由业务线程单独去处理业务逻辑。
<a name="K5Ghr"></a>
### 解决方案
面对高并发场景下，我们可以通过将请求放到阻塞队列中，通过业务线程去消费处理，达到提升我们框架的并发处理能力的需求。<br />由于RPC框架天生支持水平扩容，因此在单台机器处理能力不足的情况下，我们可以进行扩容来提升我们整个Server的能力。
<a name="MgqrW"></a>
### 落地
**请求分发处理器**<br />其中包含一个阻塞队列，在ServerHandler接收到请求后便将请求封装后放到该队列中，后续会有专门的线程池去处理这些请求。
```java
public class ServerChannelDispatcher {

    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;

    private ExecutorService executorService;

    public void init(int queueSize, int bizThreadNums) {
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(bizThreadNums, bizThreadNums,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(512));
    }

    public void add(ServerChannelReadData serverChannelReadData) {
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    public ServerChannelDispatcher() {
    }

    class ServerJobCoreHandle implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
                    executorService.submit(() -> {
                        try {
                            RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
                            RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);

                            //doFilter
                            SERVER_FILTER_CHAIN.doFilter(rpcInvocation);

                            //这里的PROVIDER_CLASS_MAP就是一开始预先在启动的时候存储的Bean集合
                            Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
                            Method[] methods = aimObject.getClass().getDeclaredMethods();
                            Object result = null;
                            for (Method method : methods) {
                                if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                                    if (method.getReturnType().equals(Void.TYPE)) {
                                        method.invoke(aimObject, rpcInvocation.getArgs());
                                    } else {
                                        result = method.invoke(aimObject, rpcInvocation.getArgs());
                                    }
                                    break;
                                }
                            }
                            rpcInvocation.setResponse(result);
                            RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
                            serverChannelReadData.getChannelHandler().writeAndFlush(respRpcProtocol);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startDataConsume(){
        Thread thread = new Thread(new ServerJobCoreHandle());
        thread.start();
    }
}
```
<a name="kfwM2"></a>
## Client端
<a name="zFMV1"></a>
### 问题
Client端对于一些不关注任务执行结果的调用，没有处理手段，其实将这个问题处理掉也是变相的提升了我们框的处理能力。
<a name="I46XZ"></a>
### 解决方案
在发起调用时增加是否异步调用，针对异步调用的请求直接返回，不再阻塞等待结果。
<a name="lOk2K"></a>
### 落地
**代码比较简单就不过多阐述了**<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672995135092-ab42d23b-2fd0-44cf-8372-6c9c92c6fe76.png#averageHue=%232e2c2b&clientId=u85d1ae6f-079d-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=634&id=u52af0c04&margin=%5Bobject%20Object%5D&name=image.png&originHeight=792&originWidth=1233&originalType=binary&ratio=1&rotation=0&showTitle=false&size=130907&status=done&style=none&taskId=ubafa5e68-40e5-4cd9-94aa-92a0a9f942c&title=&width=986.4)

![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672995167667-7f499e6a-ffd5-41ed-8331-a948c1ac5848.png#averageHue=%232e2d2c&clientId=u85d1ae6f-079d-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=712&id=uea6cf681&margin=%5Bobject%20Object%5D&name=image.png&originHeight=890&originWidth=1490&originalType=binary&ratio=1&rotation=0&showTitle=false&size=154674&status=done&style=none&taskId=u52258900-c529-4c9a-ade1-430df143530&title=&width=1192)
<a name="EFyx8"></a>
## 总结
本次版本主要是针对于Server和Client端的并发处理能力进行了优化，Server端主要是通过对请求通过队列和业务线程异步化，使得Netty的NIO线程和业务逻辑解耦，以免阻塞时间过长的逻辑影响整个RPC框架的请求接入；Client端主要是增加了对异步请求的支持，变相的提升了整个框架的处理能力。
