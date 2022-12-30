package cn.onenine.irpc.framework.core.serialize;

/**
 * Description：序列化工厂类
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/30 17:01
 */
public interface SerializeFactory {

    /**
     * 序列化
     */
    <T> byte[] serialize(T t);

    /**
     * 反序列化
     */
    <T> T deSerialize(byte[] data,Class<T> clazz);

}
