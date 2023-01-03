# IRpc **手写RPC框架第4版-序列化模块设计与实现**
<a name="tbq7B"></a>

## 为什么需要序列化？

计算机底层的传输都是通过字节流的方式进行传输的（byte[]），如果我们的内容（字符串、自定义实体、文件等等）需要传输，则需要把这些内容转换成byte[]，这一过程就是序列化；反过来如果我们想把这些byte[]再转换回目标对象，这一过程就是反序列化。
<a name="cUpSv"></a>

## 序列化常见场景

1. 网络传输：比如Dubbo、Redis通讯传输数据等
2. 磁盘IO传输：比如：本地文件->内存；内存->本地文件
3. 存储：数据的存储底层都是二进制的方式，因此数据的存取就需要进行序列化

<a name="mPkc4"></a>

## RPC框架中的应用

我们的RPC框架中需要进行网络传输，在实现中，我们自定义了Server和Client的传输协议`RpcProtocol`，并自定义了编解码器`RpcEncoder`和`RpcDecoder`，我们的RPC框架中使用了Netty网络框架，其会自动帮我们将`RpcProtocol`序列化，而在`RpcProtoco`中最重要的是`content`这个字段，我们会将`RpcInvocation`，也就是一次调用相关的内容（参数、调用方法等）都放到这个字段。<br />在`RpcProtocol`中是以`byte[]`格式存储的，因此这里我们需要考虑如何将RpcInvocation对象序列化为byte数组，供Netty进行传输。<br />常见的序列化框架有：JDK、FastJSON、Hessian、Kryo、Protocol Buf，为了兼容各种不同的序列化框架，因此我们在RPC框架中抽离了一层序列化层，专门用于对接市面上常见的序列化技术框架。<br />每种序列化框架的性能各有不同，在使用时还需要根据实际情况自行选择，后续我们会对常见的序列化框架进行性能对比测试。
<a name="EQAFG"></a>

## 序列化层实现

<a name="VOWLu"></a>

### 抽象工厂

```java
public interface SerializeFactory {
    /**
     * 序列化
     */
    <T> byte[] serialize(T t);
    /**
     * 反序列化
     */
    <T> T deserialize(byte[] data, Class<T> clazz);
}
```

<a name="N4tLt"></a>

### Kroy序列化

```java
public class KryoSerializeFactory<T> implements SerializeFactory {
    /**
     * 由于 Kryo 不是线程安全的，并且构建和配置 Kryo 实例的成本相对较高，因此在多线程环境中可能会考虑使用 ThreadLocal 或池化。
     *
     * 具体可查看Github wiki：https://github.com/EsotericSoftware/kryo#input
     */
    static private final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // Configure the Kryo instance.
            return kryo;
        };
    };
    @Override
    public <T> byte[] serialize(T t) {
        Output output = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            output = new Output(byteArrayOutputStream);
            Kryo kryo = kryos.get();
            kryo.register(t.getClass());
            kryo.writeClassAndObject(output, t);
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        Input input = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            input = new Input(byteArrayInputStream);
            Kryo kryo = kryos.get();
            return kryo.readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
}
```

<a name="TicVS"></a>

### JDK序列化

```java
public class JdkSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        byte[] data = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(os);
            output.writeObject(t);
            output.flush();
            output.close();
            data = os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("JdkSerializeFactory serialize error" ,e);
        }
        return data;
    }
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        try {
            ObjectInputStream input = new ObjectInputStream(is);
            Object result = input.readObject();
            return (T) result;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("JdkSerializeFactory deSerialize error" ,e);
        }
    }
}
```

<a name="ZyxDj"></a>

### Hessian序列化

```java
public class HessianSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        byte[] data = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(os);
            output.writeObject(t);
            output.getBytesOutputStream().flush();
            output.completeMessage();
            output.close();
            data = os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("HessianSerializeFactory serialize error", e);
        }
        return data;
    }
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null) {
            return null;
        }
        Object result = null;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            Hessian2Input input = new Hessian2Input(is);
            result = input.readObject();
        } catch (Exception e) {
            throw new RuntimeException("HessianSerializeFactory deSerialize error", e);
        }
        return (T) result;
    }
}
```

<a name="yBKCz"></a>

### FastJSON序列化

```java
public class FastJsonSerializeFactory implements SerializeFactory {
    @Override
    public <T> byte[] serialize(T t) {
        return JSONObject.toJSONString(t).getBytes();
    }
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return JSONObject.parseObject(new String(data), clazz);
    }
}
```

<a name="VMmDB"></a>

### 性能测试

<a name="fY9Hx"></a>

#### 序列化后码流大小

我们通过对一个简单的POJO进行序列化测试

```java
public class SerializeByteSizeCompareTest {

    private static User buildUserDefault() {
        User user = new User();
        user.setAge(11);
        user.setAddress("北京市昌平区");
        user.setBankNo(1215464648L);
        user.setSex(1);
        user.setId(155555);
        user.setIdCardNo("440308781129381222");
        user.setRemark("备注信息字段");
        user.setUsername("502819");
        return user;
    }

    public void jdkSerializeSizeTest(){
        SerializeFactory serializeFactory = new JdkSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        System.out.println("jdk serialize size is " + result.length);
    }
    public void hessianSerializeSizeTest(){
        SerializeFactory serializeFactory = new HessianSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        System.out.println("hessian serialize size is " + result.length);
    }
    public void kroySerializeSizeTest(){
        SerializeFactory serializeFactory = new KryoSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        System.out.println("kroy serialize size is " + result.length);
    }

    public void fastJsonSerializeSizeTest(){
        SerializeFactory serializeFactory = new FastJsonSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        System.out.println("fastJson serialize size is " + result.length);
    }

    public static void main(String[] args) {
        SerializeByteSizeCompareTest serializeByteSizeCompareTest = new SerializeByteSizeCompareTest();
        serializeByteSizeCompareTest.jdkSerializeSizeTest();
        serializeByteSizeCompareTest.hessianSerializeSizeTest();
        serializeByteSizeCompareTest.fastJsonSerializeSizeTest();
        serializeByteSizeCompareTest.kroySerializeSizeTest();
    }


}
```

测试结果如下，可见JDK序列化后的码流最大，kroy的码流最小：

```java
jdk serialize size is 448
hessian serialize size is 180
fastJson serialize size is 163
kroy serialize size is 78
```

<a name="TRXUk"></a>

#### 序列化速度

我们使用JMH来进行相关的测试。<br />JMH（Java Microbenchmark Harness）是用于代码微基准测试的工具套件，主要是基于方法层面的基准测试，精度可以达到纳秒级。该工具是由 Oracle 内部实现 JIT 的大牛们编写的，他们应该比任何人都了解 JIT 以及 JVM 对于基准测试的影响。<br />相关依赖：

```xml
<dependency>
  <groupId>org.openjdk.jmh</groupId>
  <artifactId>jmh-core</artifactId>
  <version>1.21</version>
</dependency>
<dependency>
  <groupId>org.openjdk.jmh</groupId>
  <artifactId>jmh-generator-annprocess</artifactId>
  <version>1.21</version>
  <scope>provided</scope>
</dependency>
```

测试代码：

```java
public class SerializeCompareTest {

    private static User buildUserDefault(){
        User user = new User();
        user.setAge(11);
        user.setAddress("北京市昌平区");
        user.setBankNo(1215464648L);
        user.setSex(1);
        user.setId(155555);
        user.setIdCardNo("440308781129381222");
        user.setRemark("备注信息字段");
        user.setUsername("502819");
        return user;
    }

    @Benchmark
    public void jdkSerializeTest(){
        SerializeFactory serializeFactory = new JdkSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        User deserializeUser = serializeFactory.deserialize(result,User.class);
    }

    @Benchmark
    public void hessianSerializeTest(){
        SerializeFactory serializeFactory = new HessianSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        User deserializeUser = serializeFactory.deserialize(result,User.class);
    }

    @Benchmark
    public void fastJsonSerializeTest(){
        SerializeFactory serializeFactory = new FastJsonSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        User deserializeUser = serializeFactory.deserialize(result,User.class);
    }

    @Benchmark
    public void kryoSerializeTest(){
        SerializeFactory serializeFactory = new KryoSerializeFactory();
        User user = buildUserDefault();
        byte[] result = serializeFactory.serialize(user);
        User deserializeUser = serializeFactory.deserialize(result,User.class);
    }

    public static void main(String[] args) throws RunnerException {
        //配置进行2轮热数 测试2轮 1个线程
        //预热的原因 是JVM在代码执行多次会有优化
        Options options = new OptionsBuilder().warmupIterations(2).measurementBatchSize(2)
                .forks(1).build();
        new Runner(options).run();
    }
}
```

```java
Benchmark                                    Mode  Cnt       Score       Error  Units
SerializeCompareTest.fastJsonSerializeTest  thrpt    5  394505.209 ± 30504.962  ops/s
SerializeCompareTest.hessianSerializeTest   thrpt    5  131266.505 ± 18162.446  ops/s
SerializeCompareTest.jdkSerializeTest       thrpt    5   27091.395 ±   901.560  ops/s
SerializeCompareTest.kryoSerializeTest      thrpt    5  362255.747 ±  3446.206  ops/s
```

从结果上看来，FastJSON的序列化吞吐量最好的，JDK还是最差的。
<a name="TeQtx"></a>

## RPC框架整合

<a name="bHhb5"></a>

### 配置化序列化方式

```properties
#provider端口
irpc.serverPort=8098
#注册中心地址
irpc.registerAddr=localhost:2181
#provider 应用名称
irpc.applicationName=irpc-provider
#代理方式
irpc.proxyType=jdk
#调用超时时间
irpc.call.timeout=30000
#provider ip 路由策略
irpc.routerStrategy=rotate
#序列化方式
irpc.serverSerialize=fastJson
irpc.clientSerialize=fastJson
```

**需要整合的点：**
<a name="ya84R"></a>

### Client调用时

```java
class AsyncSendJob implements Runnable {
    public AsyncSendJob() {
    }
    @Override
    public void run() {
        while (true) {
            try {
                //阻塞模式
                RpcInvocation data = CommonClientCache.SEND_QUEUE.take();
                //将RpcInvocation封装到RpcProtocol对象中，然后发送给服务端，这里正好对应了ServerHandler
                RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(data));
                ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(data.getTargetServiceName());
                channelFuture.channel().writeAndFlush(rpcProtocol);
            } catch (Exception e) {
                LOGGER.error("client call error", e);
            }
        }
    }
}
```

<a name="fyATi"></a>

### Client接收到Server响应后

```java
public class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] reqContent = rpcProtocol.getContent();
        RpcInvocation rpcInvocation = CLIENT_SERIALIZE_FACTORY.deserialize(reqContent, RpcInvocation.class);
        //通过之前发送的uuid来注入匹配的响应数值
        if (!RESP_MAP.containsKey(rpcInvocation.getUuid())){
            throw new IllegalArgumentException("server response is error");
        }
        //将请求的响应结构放入一个Map集合中，集合的key就是uuid，这个uuid在发送请求之前就已经初始化好了
        //所以只需要起一个线程在后台遍历这个map，查看对应的key是否有响应即可
        //uuid放入map的操作被封装到了代理类中进行实现
        RESP_MAP.put(rpcInvocation.getUuid(),rpcInvocation);
        ReferenceCountUtil.release(msg);
    }
}
```

<a name="NwDvD"></a>

### Server端接收到Client的请求和返回响应时

```java
public class ServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //服务端接收数据的时候以RpcProtocol协议的格式接收
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(),RpcInvocation.class);
        //这里的PROVIDER_CLASS_MAP就是一开始预先在启动的时候存储的Bean集合
        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
        Method[] methods = aimObject.getClass().getMethods();
        Object result = null;
        for (Method method : methods) {
            if (method.getName().equals(rpcInvocation.getTargetMethod())){
                if (method.getReturnType().equals(Void.TYPE)){
                    method.invoke(aimObject,rpcInvocation.getArgs());
                }else {
                    result = method.invoke(aimObject,rpcInvocation.getArgs());
                }
                break;
            }
        }
        rpcInvocation.setResponse(result);
        RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
        ctx.writeAndFlush(respRpcProtocol);
    }
}
```



