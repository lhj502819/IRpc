package cn.onenine.irpc.framework.core.registy;

/**
 * Description：负责服务的注册、下线、订阅、取消订阅
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/18 19:14
 */
public interface RegistryService {

    /**
     * 注册URL，将irpc服务写入注册中心节点
     * 当网络都得的时候需要进行适当的重试做法
     * 注册服务url的时候需要写入持久化文件中
     */
    void register(URL url);

    /**
     * 服务下线，当某个服务提供者要下线了，则需要将主动将注册过的服务信息从zk指定节点上摘除
     * 此时就需要调用unRegister接口
     *
     * 持久化节点是无法进行服务下线操作的
     * 下线的服务必须保证url是完整匹配的
     * 移除持久化文件中的一些内容信息
     */
    void unRegister(URL url);

    /**
     * 消费方订阅接口
     * 订阅某个服务，通常是客户端在启动阶段需要调用的接口。客户端在启动过程中需要调用该函数
     *   从注册中心中提取现有的服务提供者地址
     */
    void subscribe(URL url);

    /**
     * 取消订阅服务，当服务调用方不打算再继续订阅某些服务的时候，就需要调用该方法去取消服务的订阅功能
     *  将注册中心的订阅记录移除
     * @param url
     */
    void doUnSubscribe(URL url);



}
