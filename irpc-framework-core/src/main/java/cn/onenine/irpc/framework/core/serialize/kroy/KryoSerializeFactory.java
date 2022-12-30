package cn.onenine.irpc.framework.core.serialize.kroy;

import cn.onenine.irpc.framework.core.serialize.SerializeFactory;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Description：Kryo序列化工厂
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/30 17:35
 */
public class KryoSerializeFactory implements SerializeFactory {

    private static final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            return kryo;
        }
    };

    @Override
    public <T> byte[] serialize(T t) {
        Output output = null;
        try {
            Kryo kryo = kryos.get();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            output = new Output(byteArrayOutputStream);
            kryo.writeClassAndObject(output, t);
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("KryoSerializeFactory serialize error", e);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        Input input = null;
        try {
            Kryo kryo = kryos.get();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            input = new Input(byteArrayInputStream);
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new RuntimeException("KryoSerializeFactory deSerialize error", e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
}
