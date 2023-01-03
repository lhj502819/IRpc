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

    public void setAsync(boolean async){
        this.attatchments.put("async",async);
    }

    public boolean isAsync(){
        return (boolean) this.attatchments.get("async");
    }

}
