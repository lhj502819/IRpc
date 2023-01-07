package cn.onenine.irpc.framework.core.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description：RPC远程调用包装类
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/3 22:14
 */
public class RpcReferenceWrapper<T> {

    private Class<T> aimClass;

    private Map<String, Object> attatchments = new ConcurrentHashMap<>();

    public Class<T> getAimClass() {
        return aimClass;
    }

    public void setAimClass(Class<T> aimClass) {
        this.aimClass = aimClass;
    }

    public Map<String, Object> getAttatchments() {
        return attatchments;
    }

    public void setGroup(String group) {
        this.attatchments.put("group", group);
    }

    public void setToken(String token) {
        this.attatchments.put("token", token);
    }

    /**
     * 设置是否异步执行，默认为同步
     */
    public void setAsync(boolean async){
        this.attatchments.put("async",async);
    }

    /**
     * 是否异步执行
     */
    public boolean isAsync(){
        Object async = this.attatchments.get("async");
        if (async == null) {
            return false;
        }
        return (boolean) async;
    }


    public String getUrl() {
        return String.valueOf(attatchments.get("url"));
    }

    public void setUrl(String url) {
        attatchments.put("url", url);
    }

    public void setTimeOut(int timeOut) {
        attatchments.put("timeOut", timeOut);
    }

    public String getTimeOUt() {
        return String.valueOf(attatchments.get("timeOut"));
    }

    /**
     * 失败重试次数
     */
    public int getRetry(){
        if(attatchments.get("retry")==null){
            return 0;
        }else {
            return (int) attatchments.get("retry");
        }
    }

    public void setRetry(int retry){
        this.attatchments.put("retry",retry);
    }

}
