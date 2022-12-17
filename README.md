# IRpc **手写RPC框架第一版**

## 支持的功能如下

### 自定义协议体解决网络粘包拆包的问题：`RpcProtocol`

``````Java
public class RpcProtocol implements Serializable {

    private static final long serialVersionUID = 8036047420171733802L;

    /**
     * 魔法数，主要在做服务通讯的时候定义的一个安全监测，确认当前请求的协议是否合法
     */
    private short magicNumber = RpcConstants.MAGIC_NUMBER;

    /**
     * 协议传输核心数据的长度，这里将长度单独拎出来有个好处，
     *  当服务端的接收能力有限的时候，可以对该字段进行赋值。
     *  当读取到网络数据包中的contentLength字段已经超过预期值的话，就不会去读取content字段
     */
    private int contentLength;

    /**
     * 核心传输数据，这里核心的传输数据主要是请求的服务名称，请求服务的方法名称，请求参数内容。
     *  为了方便后期扩展，这些核心的请求数据都统一封装到了RpcInvocation
     */
    private byte[] content;

    public RpcProtocol(byte[] content) {
        this.content = content;
        this.contentLength = content.length;
    }
	//........setter、getter、toString...........
}
``````



### 自定义序列化/反序列化方式：`RpcEncoder`、`RpcDecoder`

``````Java
public class RpcEncoder extends MessageToByteEncoder<RpcProtocol> {
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcProtocol msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagicNumber());
        out.writeInt(msg.getContentLength());
        out.writeBytes(msg.getContent());
    }
}
``````

``````
/**
 * Description：解码器
 *  在实现过程中需要考虑是否会有粘包拆包的问题，并且还要设置请求数据包体积最大值
 *  处理粘包拆包问题方案见{@see https://mp.weixin.qq.com/s/oN-gBB8eYn4rJH82YlD5Rw}
 */
public class RpcDecoder extends ByteToMessageDecoder {

    /**
     * 协议的开头部分的标注长度
     */
    public final int BASE_LENGTH = 2 + 4;

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= BASE_LENGTH){
            if (in.readableBytes() > 1000){
                //防止收到一些体积过大的数据包
                in.skipBytes(in.readableBytes());
            }
            int beginReader;
            while (true){
                //第一次为0
                beginReader = in.readerIndex();
                //标记readerIndex，可以通过resetReaderIndex，重新将buffer的readerIndex重置为标记的readerIndex
                in.markReaderIndex();
                //这里对应了RpcProtocol的魔数
                if (in.readShort() == MAGIC_NUMBER){
                    break;
                }else {
                    //如果不是魔数开头，说明是非法的客户端发来的数据包
                    ctx.close();
                    return;
                }
            }

            //这里对应RpcProtocol的contentLength字段
            int contentLength = in.readInt();
            //说明剩余的数据包不是完整的，这里需要重置下readerIndex
            if (in.readableBytes() < contentLength){
                in.readerIndex(beginReader);
                return;
            }

            //这里其实就是实际的RpcProtocol对象的content字段
            byte[] data = new byte[contentLength];
            in.readBytes(data);
            RpcProtocol rpcProtocol = new RpcProtocol(data);
            out.add(rpcProtocol);
        }
    }
}
``````



### 客户端异步消费任务队列实现消息发送，通过uuid来标识请求线程和响应线程之间的数据匹配问题

``````
/**
 * 开启发送线程，专门从事将数据包发送给服务端，起到一个解耦的作用
 */
private void startClient(ChannelFuture channelFuture) {
    //请求发送任务交给单独的IO线程去负责，实现异步化，提升发送性能
    Thread asyncSendJob = new Thread(new AsyncSendJob(channelFuture));
    asyncSendJob.start();
}

/**
 * 异步发送信息任务
 */
class AsyncSendJob implements Runnable {

    private ChannelFuture channelFuture;

    public AsyncSendJob(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    public void run() {
        while (true) {
            try {
                //阻塞模式
                RpcInvocation data = CommonClientCache.SEND_QUEUE.take();
                //将RpcInvocation封装到RpcProtocol对象中，然后发送给服务端，这里正好对应了ServerHandler
                String json = JSONObject.toJSONString(data);
                RpcProtocol rpcProtocol = new RpcProtocol(json.getBytes());
                //netty的通道负责发送数据给服务端
                channelFuture.channel().writeAndFlush(rpcProtocol);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
``````

### 通过动态代理的方式完成方法的拦截调用：`JDKClientInvocationHandler`

``````Java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    RpcInvocation rpcInvocation = new RpcInvocation();
    rpcInvocation.setArgs(args);
    rpcInvocation.setTargetMethod(method.getName());
    rpcInvocation.setTargetServiceName(clazz.getName());
    //这里面注入了一个uuid，对每一次的请求都单独区分
    rpcInvocation.setUuid(UUID.randomUUID().toString());
    RESP_MAP.put(rpcInvocation.getUuid(),OBJECT);
    SEND_QUEUE.add(rpcInvocation);
    long beginTime = System.currentTimeMillis();
    //客户端请求超时的判断依据
    while (System.currentTimeMillis() - beginTime < 3 * 1000){
        Object object = RESP_MAP.get(rpcInvocation.getUuid());
        if (object instanceof  RpcInvocation){
            return ((RpcInvocation)object).getResponse();
        }
    }

    throw new TimeoutException("client wait server's response timeout!");
}
``````



