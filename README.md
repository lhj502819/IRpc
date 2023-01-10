# IRpc 手写RPC框架第8版-通过Springboot-starter集成SpringBoot
<a name="wh5qv"></a>
## 为什么要接入Spring/SpringBoot？
目前Spring和SpringBoot在Java领域应该是主流的开发框架了，国内的小伙伴应该是无人不用，唯一的差异点可能就是有的人还没有使用过SpringBoot，但Spring肯定是事实标准了。<br />Spring/SpringBoot有啥优点呢？简单来说是它帮我们减少了很多不必要的工作，比如第三方中间件的集成、框架的集成，它都有一套自己的封装，因此目前主流的框架或者中间件都会去适配Spring/SpringBoot，一方面为了自己框架的推广，另一方面也是为了使用Spring/SpringBoot的特性，让框架的使用更加简单便捷。<br />那是怎么让框架的使用更加简单便捷的呢？首先我们就要先了解我们框架目前使用的困难点，可以看到我们发起一次调用都要写这么复杂的代码。<br />![在这里插入图片描述](https://img-blog.csdnimg.cn/bbed655e0dd844009d8227f3e02e6793.png)
<br />那通过集成Spring/SpringBoot能达到什么效果呢？我们先看下接入后的使用流程，如下图，对你没有看错，就是这么简单，相信使用过Dubbo的朋友一定很熟悉，就是借鉴的Dubbo~<br />


<center><img src="https://img-blog.csdnimg.cn/302f04acbc934a7885f8de8b667b7d28.png" ></center>

![在这里插入图片描述](https://img-blog.csdnimg.cn/7c95fddbee884c43a07af69acf9a4ac6.png)
<br />接下来让我们一起看看怎么操作吧！<br />
<center><img src="https://img-blog.csdnimg.cn/c99f61d36f674a6ca02d9b197b054d41.png" width="180"></center>

<a name="xhkrq"></a>
## 自定义Spingboot starter
这里我们就不去实现对Spring的集成了，直接上高阶的，因为Spring的比较麻烦哈哈，目前主流的应该也是Springboot了。
<a name="Ve4br"></a>
### 如何自定义Springboot starter
我们需要在META-INF中创建自动装配配置文件`spring.factories`，文件内容如下：
```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.onenine.irpc.framework.springboot.starter.config.IRpcServerAutoConfiguration,\
cn.onenine.irpc.framework.springboot.starter.config.IRpcClientAutoConfiguration
```
`EnableAutoConfiguration`为SpringBoot的注解，见名知意就是开启自动配置的意思，其`=`号后边的就是我们需要装配的类，通过这样的方式，我们将starter项目编译打包之后推送到maven仓库，他人在便可以通过引用我们的starter maven坐标来集成RPC框架，具体的使用方式会在后边进行演示。<br />
<center><img src="https://img-blog.csdnimg.cn/78b919d1445944769e874a9543c629f9.png" width=""></center>

<a name="Za9ES"></a>
### Client端自动装配/配置实现
<a name="HbuyN"></a>
#### 自定义Reference注解
为了我们能一眼就认出哪些属性需要我们进行动态代理（比如DataService dataService），因此我们需要设计一个注解来标识，并且我们还可以把该Service的调用相关参数通过注解进行配置，这样我们在自动装配的时候直接读取注解的元信息再去执行相关的封装操作岂不美哉。<br />
<center><img src="https://img-blog.csdnimg.cn/f43b9ec805a34c3bb0d67c93b88c0166.png" width=""></center>

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IRpcReference {

    String url() default "";

    String group() default "";

    String serviceToken() default "";

    int timeOut() default 3000;

    int retry() default 1;

    boolean async() default false;
}
```
可以看到和这里的属性是一一对应的。<br />

<center><img src="https://img-blog.csdnimg.cn/40023310d5134a848680e17533361501.png" width=""></center>

<a name="s2C8H"></a>
#### 自动装配逻辑
接下来该咋办呢？我们都知道在使用Spring的时候都需要去定义`Controller/Service/DAO`，在里边去注入我们需要的其他Bean，并且我们的`Controller/Service/DAO`也会作为一个Bean被Spring容器管理起来，那我们就需要想办法在Bean创建完之后，获取到有标记`IRpcReference`注解属性的Bean，再执行后续的生成代理对象，获取Provider和建立连接的逻辑。<br />在Spring中为我们提供了一些列的扩展点，其中有一个接口`BeanPostProcessor`，该接口提供了两个方法，如下：
```java
public interface BeanPostProcessor {
    //Bean实例化之前执行
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    //Bean实例化之后执行
    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
```
那这里很明显我们就要去实现`postProcessAfterInitialization`去完成我们的相关逻辑，以下是代码实现，比较简单，就不过多解释了：<br />

<center><img src="https://img-blog.csdnimg.cn/c37f6224071940878dd56e805a750c2c.png" width=""></center>


```java
public class IRpcClientAutoConfiguration implements BeanPostProcessor, ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRpcClientAutoConfiguration.class);

    private Client client;

    private RpcReference rpcReference;

    /**
     * 是否需要启动NettyClient
     */
    private boolean hasInitClientApplication;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                //设置私有变量可访问
                field.setAccessible(true);
                IRpcReference rpcReferenceAnnotation = field.getAnnotation(IRpcReference.class);
                if (rpcReferenceAnnotation == null) {
                    continue;
                }
                if (!hasInitClientApplication){
                    try {
                        client = new Client();
                        rpcReference = client.initClientApplication();
                    } catch (Exception e) {
                        LOGGER.error("init and start netty client error" , e);
                        throw new RuntimeException(e);
                    }
                }
                hasInitClientApplication = true;
                RpcReferenceWrapper rpcReferenceWrapper = new RpcReferenceWrapper();
                rpcReferenceWrapper.setAimClass(field.getType());
                rpcReferenceWrapper.setGroup(rpcReferenceAnnotation.group());
                rpcReferenceWrapper.setTimeOut(rpcReferenceAnnotation.timeOut());
                rpcReferenceWrapper.setToken(rpcReferenceAnnotation.serviceToken());
                rpcReferenceWrapper.setUrl(rpcReferenceAnnotation.url());
                rpcReferenceWrapper.setRetry(rpcReferenceAnnotation.retry());
                rpcReferenceWrapper.setAsync(rpcReferenceAnnotation.async());
                field.set(bean,rpcReference.get(rpcReferenceWrapper));
                //订阅服务，提前获取到所有的service Provider
                client.doSubscribeService(field.getType());
            } catch (Throwable e) {
                throw new RuntimeException("[IRpcClientAutoConfiguration#postProcessAfterInitialization] init rpcReference error", e);
            }
        }
        return bean;
    }

    public void onApplicationEvent(ApplicationReadyEvent event) {
       if (hasInitClientApplication){
           ConnectionHandler.setBootstrap(client.getBootstrap());
           client.doConnectServer();
           client.startClient();
       }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println(applicationContext);
    }
}
```
<a name="AfJjk"></a>
### Server端自动装配/配置实现
<a name="JC0Ta"></a>
#### 自定义IRpcService注解
Client端我们整了个注解来解决自动装配需要远程调用的属性字段，那Server端需要做啥呢？首先我们需要考虑的是Server端其实就是将我们的Service注册到注册中心，创建Socket等待连接接入和处理IO请求，对于Socket我们使用Netty框架来完成NIO的操作，而唯一需要简化的便是如何简单的将需要暴露注册的Service更加简单的注册到注册中心，其实最简单的方式还是通过注解去标识，在自动装配逻辑中去获取到标记了这些注解的Class，自动执行后续的注册逻辑。<br />


<center><img src="https://img-blog.csdnimg.cn/b1491361f5cb4da0be3f3d43c1080dd2.png" width="300"></center>

<br />自定义注解如下：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface IRpcService {

    int limit() default 0;

    String group() default "";

    String serviceToken() default "";

}

```
其实和我们的直接使用API还是一一对应的：<br />
![在这里插入图片描述](https://img-blog.csdnimg.cn/cc98c2d7db0f4881bb3cd1cf0da30b17.png)

<a name="Be8z5"></a>
#### 自动装配逻辑
自动装配这里的逻辑还是比较简单，我们也就不过多解释了，上代码：
```java
public class IRpcServerAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRpcServerAutoConfiguration.class);

    private ApplicationContext applicationContext;

    public void afterPropertiesSet() throws Exception {
        Server server = null;
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(IRpcService.class);
        if (beansWithAnnotation.isEmpty()) {
            //没有带暴露的服务
            return;
        }

        printBanner();
        long start = System.currentTimeMillis();
        server = new Server();
        server.initServerConfig();
        for (String beanName : beansWithAnnotation.keySet()) {
            Object beanObject = beansWithAnnotation.get(beanName);
            IRpcService iRpcService = beanObject.getClass().getAnnotation(IRpcService.class);
            ServiceWrapper serviceWrapper = new ServiceWrapper(beanObject, iRpcService.group());
            serviceWrapper.setLimit(iRpcService.limit());
            serviceWrapper.setServiceToken(iRpcService.serviceToken());
            server.exportService(serviceWrapper);
            LOGGER.info("service {} export success!", beanName);
        }
        long end = System.currentTimeMillis();
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
        LOGGER.info("{} start success in {} times", server.getConfig(), end - start);
    }

    private void printBanner() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("|||---------- IRpc Starting Now! ----------|||");
        System.out.println("==============================================");
        System.out.println("源代码地址: https://github.com/lhj502819/IRpc");
        System.out.println("version: 1.9.0");
        System.out.println();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
```
<a name="kpLVE"></a>
### 待优化项
目前我们只是对Client和Server的目标Service和待暴露Service进行了自动装配，而却少了对相关的配置与SpringBoot集成，本打算去集成一下试试，鉴于时间的原因一直没做，大家可以参照比较流行的框架，如SpringCloudGateway去仿照着实现下，但我们自动装配的方式可能就需要调整下，感兴趣的可以去研究研究，但是目前的实现方式不影响大家理解！<br />

<center><img src="https://img-blog.csdnimg.cn/ba1d9704ed1e4ae68ee72902271d0f9e.png" width="180"></center>


<a name="QDFZE"></a>
## 接入一个SpringBoot项目
首先需要引入相关的Maven依赖，irpc-starter和springboot-web为主，其他的大家看项目需要。
```java
<dependency>
    <groupId>cn.onenine</groupId>
    <artifactId>irpc-framework-spring-start</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>2.7.0</version>
</dependency>
```
其次我们在配置完相关Client/Server的配置后就可以直接使用了。
<a name="pIMB0"></a>
### Client
![在这里插入图片描述](https://img-blog.csdnimg.cn/87a7a3e99dd0485d87a151f77a1f178a.png)

<a name="x5Xle"></a>
### Server
![在这里插入图片描述](https://img-blog.csdnimg.cn/bdd797fc9b5c42a4b45bf02c8d7e3eb0.png)
<br />是不是很简单，就是这么简单，大家自行去看源码即可~<br />


<a name="VJEh8"></a>
## 总结
本次我们主要通过自定义的Springboot-starter来完成了我们RPC框架对Springboot的集成，方便了框架的接入和使用成本，大家还有什么需要直接在基础上扩展即可，新年即将来临，别的也不祝福了，就祝大家身体健康吧，目前虽然很难，让我们一起前行，共渡难关！<br />

<center><img src="https://img-blog.csdnimg.cn/28b23c5f1a1d44feb6d102789618a962.png" width=""></center>
