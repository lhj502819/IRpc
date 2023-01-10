# IRpc 手写RPC框架v6-通过SPI机制增加框架的扩展性的设计与实现
<a name="Lhk5K"></a>
## 现有的问题
在上一章节末尾我们提到了，目前我们的RPC框架可扩展性还不太友好，用户如果想自定义一个过滤器或者序列化方式还需要去修改源码。本次我们就通过SPI的机制去解决这个问题。
<a name="nVr6q"></a>
## 什么是SPI？
SPI全称`Service Provider Interface`，是Jdk提供的一种用来扩展框架的服务能力的机制，它能够在运行时将我们定义的类加载到JVM中并实例化。通常面向对象编程推荐的是面向接口编程，而SPI机制就需要先定义好接口，后续对接口进行实现，而如果我们想要替换实现或者增加接口实现的的话，一般都需要修改源代码，SPI机制就是来解决这个问题的，在运行时可以动态的去加载我们配置的Class，将其装配到框架中去。
<a name="sYzrq"></a>
## 常见的SPI实现
<a name="PIKeO"></a>
### jdk原生
Jdk从1.6起引入了SPI机制，我们需要在指定目录`META-INF/services`下创建我们SPI的文件，文件名称为需要扩展的接口全限定名，如：`cn.onenine.irpc.framework.core.router.IRouter`，将自定义的实现类配置到里边，如：`cn.onenine.irpc.framework.core.router.RandomRouterImpl`，这样我们就可以使用Jdk的API去获取到我们自定义的类对象。
<a name="aDW3m"></a>
#### 使用方式
**可扩展接口定义**
```java
public interface ISpiTest {

    void doSomething();

}
```
**自定义实现**
```java
public class DefaultISpiTest implements ISpiTest{
    @Override
    public void doSomething() {
        System.out.println("执行测试方法");
    }
}
```

**SPI配置文件**<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672886346604-e7acf355-6c45-4ed4-8a13-c43f9fc924a7.png#averageHue=%23384538&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=222&id=u0af1811d&margin=%5Bobject%20Object%5D&name=image.png&originHeight=277&originWidth=1376&originalType=binary&ratio=1&rotation=0&showTitle=false&size=24791&status=done&style=none&taskId=u96af2384-ffff-4bda-8226-79bdd9875bf&title=&width=1100.8)<br />**集成代码**
```java
public static void main(String[] args) {
    ServiceLoader<ISpiTest> serviceLoader = ServiceLoader.load(ISpiTest.class);
    Iterator<ISpiTest> iSpiTestIterator = serviceLoader.iterator();
    while (iSpiTestIterator.hasNext()) {
        ISpiTest iSpiTest = iSpiTestIterator.next();
        TestSpiDemo.doTest(iSpiTest);
    }
}
```
<a name="bK3yA"></a>
#### 实现原理
Jdk的SPI会在执行`iterator#hasNext`的时候去加载相关的类信息<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672888187187-0ca190a7-c696-4227-9bfd-32d09a03dc75.png#averageHue=%234c5657&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=584&id=ud328678c&margin=%5Bobject%20Object%5D&name=image.png&originHeight=730&originWidth=1807&originalType=binary&ratio=1&rotation=0&showTitle=false&size=140830&status=done&style=none&taskId=uf346ca20-a7b6-4ec6-a894-1d4dca6fba7&title=&width=1445.6)<br />读取到我们定义的文件后，会将文件内容读取出来，将Class的全限定名保存，在调用`Iterator#next`时才会创建类对象。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672888375730-4277e345-3056-454d-8459-2f44885a9fec.png#averageHue=%236a836a&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=677&id=ub4a6ed64&margin=%5Bobject%20Object%5D&name=image.png&originHeight=846&originWidth=1818&originalType=binary&ratio=1&rotation=0&showTitle=false&size=188637&status=done&style=none&taskId=u276ee9c1-470d-4142-9bd1-16bf6d69417&title=&width=1454.4)
<a name="iJw8h"></a>
#### 实际应用
我们在使用原生MySQL的JDBC的时候，都知道有个API叫`DriverManager`，它就是通过SPI的方式去加载Jdk提供的`java.sql.Driver`实现类，具体的配置如下，我使用的8.0驱动，其他版本的可能会有些许不同。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672889201052-f77ed570-11ac-49af-bf13-caf0e68525e1.png#averageHue=%23504e43&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=414&id=ua65d45de&margin=%5Bobject%20Object%5D&name=image.png&originHeight=518&originWidth=1469&originalType=binary&ratio=1&rotation=0&showTitle=false&size=65798&status=done&style=none&taskId=u02e4c6ab-85ec-4b82-be6f-41a3c86111b&title=&width=1175.2)<br />`DriverManager`中有静态代码块去加载对应的类实例<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672889276374-b77e38cd-c8c2-4cc8-96a0-3503b6b589e4.png#averageHue=%232c2c2b&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=210&id=u31be162b&margin=%5Bobject%20Object%5D&name=image.png&originHeight=263&originWidth=775&originalType=binary&ratio=1&rotation=0&showTitle=false&size=28332&status=done&style=none&taskId=u1fb1bb77-e7f8-4cdc-8338-1d50789861a&title=&width=620)<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672889294405-65d3bc34-21e1-4ddc-89a5-cb8c3bd557b0.png#averageHue=%232d2c2b&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=541&id=u1b2f9b7f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=676&originWidth=961&originalType=binary&ratio=1&rotation=0&showTitle=false&size=89406&status=done&style=none&taskId=u96d6dd09-a8ed-448b-8829-d6a0cbc800e&title=&width=768.8)<br />最终jdbc Driver在初始化时会将自身注册到DriverManager中，供`DriverManager#getConnection`使用。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672891626601-3dcdf548-fef3-43eb-9da2-a77eb6aca756.png#averageHue=%232d2b2b&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=655&id=u24a24a60&margin=%5Bobject%20Object%5D&name=image.png&originHeight=819&originWidth=951&originalType=binary&ratio=1&rotation=0&showTitle=false&size=75889&status=done&style=none&taskId=u8771ae1f-87d3-42ad-8c63-ff0e414a26e&title=&width=760.8)
<a name="Zp89l"></a>
#### 缺点

- 加载实现的时候是通过迭代器把所有配置的实现都加在一遍，无法做到按需加载，如果某些不想使用的类实例化很耗时，就会造成资源的浪费了；
- 第一个点引发的问题：获取某个实现类方式不灵活，不能通过参数控制要加载什么类，每次都只能迭代获取。而在一些框架的运行时通过参数控制加载具体的类的需求是很有必要的；
- 最后一点，ServiceLoader类的实例用于多个并发线程是不安全的。比如LazyIterator::nextService中的providers.put(cn, p);方法不是线程安全的。

基于这些缺点，目前很多中间件或者框架都会选择自行实现SPI机制，这里我们的RPC框架中也来实现一个自己的SPI，主要思路借鉴Dubbo框架。
<a name="du8za"></a>
### 自定义SPI实现
SPI的主要实现思路其实就是通知设置某种规则，将需要扩展的类配置到指定目录下，通过程序读取到指定的配置后，将类进行实例化，供框架使用。<br />为了实现SPI使用的灵活性，我们将SPI配置文件中的内容调整为key-value的格式，key为扩展的具体功能名称，value为对应类的全限定名，这样在使用的时候我们可以通过应用的配置文件去指定要创建的组件名称，和SPI机制打通，增加使用的灵活性。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/1171730/1672892086150-dc2f365f-07c7-4915-9ea1-37852bb071e8.png#averageHue=%238e9d7f&clientId=ude24e297-2c28-4&crop=0&crop=0&crop=1&crop=1&from=paste&height=482&id=ua6b2cb44&margin=%5Bobject%20Object%5D&name=image.png&originHeight=603&originWidth=1894&originalType=binary&ratio=1&rotation=0&showTitle=false&size=69045&status=done&style=none&taskId=ua9ab8124-299b-41d4-ab24-0da0f8cf1ed&title=&width=1515.2)<br />具体SPI的加载代码如下，比较简单，不过多阐述：
```java
public class ExtensionLoader {

    public static String EXTENSION_LOADER_DIR_PREFIX = "META-INF/irpc/";

    /**
     * key：interface name  value：{key:configName value:ImplClass}
     */
    public static Map<String, LinkedHashMap<String, Class>> EXTENSION_LOADER_CLASS_CACHE = new ConcurrentHashMap<>();

    public void loadExtension(Class clazz) throws IOException, ClassNotFoundException {
        if (clazz == null) {
            throw new IllegalArgumentException("class can not null");
        }

        String spiFilePath = EXTENSION_LOADER_DIR_PREFIX + clazz.getName();
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> enumeration = classLoader.getResources(spiFilePath);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            InputStreamReader inputStreamReader = null;
            inputStreamReader = new InputStreamReader(url.openStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            LinkedHashMap<String, Class> classMap = new LinkedHashMap<>();
            while ((line = bufferedReader.readLine()) != null) {
                //如果配置中加入了#开头，则表示忽略该类，无需加载
                if (line.startsWith("#")){
                    continue;
                }
                String[] lineArr = line.split("=");
                String implClassName = lineArr[0];
                String interfaceName = lineArr[1];
                //保存的同时初始化类
                classMap.put(implClassName,Class.forName(interfaceName));
            }

            //放入缓存中
            if (EXTENSION_LOADER_CLASS_CACHE.containsKey(clazz.getName())){
                EXTENSION_LOADER_CLASS_CACHE.get(clazz.getName()).putAll(classMap);
            }else {
                EXTENSION_LOADER_CLASS_CACHE.put(clazz.getName(),classMap);
            }
        }
    }

}
```
<a name="a1XAy"></a>
## RPC框架接入
我们的RPC框架目前可扩展或指定的功能有如下：

- 序列化方式
- 路由策略
- 过滤器
- 注册中心
- 动态代理实现（我们目前使用的默认JDK动态代理，还有其他的代理方式，如CGLIB等）
  <a name="AfYgT"></a>
### Client端调整
我们将可扩展的点都通过SPI的方式去配置，方便用户去集成我们的框架，Server端的同理，这里就不过多展示了，大家去看源码即可。
```java
private void initConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    //初始化路由策略
    EXTENSION_LOADER.loadExtension(IRouter.class);
    String routeStrategy = CLIENT_CONFIG.getRouteStrategy();
    LinkedHashMap<String, Class> iRouterMap = EXTENSION_LOADER_CLASS_CACHE.get(IRouter.class.getName());
    Class iRouterClass = iRouterMap.get(routeStrategy);
    if (iRouterClass == null) {
        throw new RuntimeException("no match routerStrategy for " + routeStrategy);
    }
    IROUTER = (IRouter) iRouterClass.newInstance();
    //初始化序列化方式
    EXTENSION_LOADER.loadExtension(SerializeFactory.class);
    String serializeType = CLIENT_CONFIG.getClientSerialize();
    LinkedHashMap<String, Class> serializeTypeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
    Class serializeClass = serializeTypeMap.get(serializeType);
    if (serializeClass == null) {
        throw new RuntimeException("no match serialize type for " + serializeType);
    }
    CLIENT_SERIALIZE_FACTORY = (SerializeFactory) serializeClass.newInstance();
    //初始化过滤链
    EXTENSION_LOADER.loadExtension(IClientFilter.class);
    ClientFilterChain clientFilterChain = new ClientFilterChain();
    LinkedHashMap<String, Class> filterMap = EXTENSION_LOADER_CLASS_CACHE.get(IClientFilter.class.getName());
    for (String implClassName : filterMap.keySet()) {
        Class filterClass = filterMap.get(implClassName);
        if (filterClass == null) {
            throw new NullPointerException("no match client filter for " + implClassName);
        }
        clientFilterChain.addServerFilter((IClientFilter) filterClass.newInstance());
    }
    CLIENT_FILTER_CHAIN = clientFilterChain;
}
```
<a name="lyhA2"></a>
## 总结
本版本我们对SPI机制进行了详解，并且自己实现了SPI机制，增加了原Jdk原生的SPI机制的不足，并集成在了我们的RPC框架中，后续如果想对框架中的功能进行扩展的话，通过SPI机制无需修改源代码即可完成。
