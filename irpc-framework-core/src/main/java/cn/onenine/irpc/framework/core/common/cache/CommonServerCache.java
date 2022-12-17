package cn.onenine.irpc.framework.core.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/16 23:04
 */
public class CommonServerCache {

    public static Map<String,Object> PROVIDER_CLASS_MAP = new ConcurrentHashMap<String, Object>();



}
