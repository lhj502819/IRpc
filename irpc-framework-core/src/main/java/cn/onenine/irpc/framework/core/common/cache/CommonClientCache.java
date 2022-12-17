package cn.onenine.irpc.framework.core.common.cache;

import cn.onenine.irpc.framework.core.common.RpcInvocation;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description：公共缓存，存储请求队列等公共信息
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 13:33
 */
public class CommonClientCache {

    public static BlockingQueue<RpcInvocation> SEND_QUEUE = new ArrayBlockingQueue<RpcInvocation>(100);

    public static Map<String,Object> RESP_MAP = new ConcurrentHashMap<String, Object>();

}
