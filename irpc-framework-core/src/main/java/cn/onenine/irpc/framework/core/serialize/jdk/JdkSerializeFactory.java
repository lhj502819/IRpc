package cn.onenine.irpc.framework.core.serialize.jdk;

import cn.onenine.irpc.framework.core.serialize.SerializeFactory;

import java.io.*;

/**
 * Description：Jdk原生序列化
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/30 17:14
 */
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
