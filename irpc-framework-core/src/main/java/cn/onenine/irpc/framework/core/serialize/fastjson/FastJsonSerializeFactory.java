package cn.onenine.irpc.framework.core.serialize.fastjson;

import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import com.alibaba.fastjson2.JSONObject;

/**
 * Description：使用FastJson2序列化
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/30 17:11
 */
public class FastJsonSerializeFactory implements SerializeFactory {

    @Override
    public <T> byte[] serialize(T t) {
        return JSONObject.toJSONString(t).getBytes();
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        return JSONObject.parseObject(new String(data), clazz);
    }
}
