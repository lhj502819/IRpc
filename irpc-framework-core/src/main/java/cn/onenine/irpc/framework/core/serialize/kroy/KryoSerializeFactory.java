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
public class KryoSerializeFactory<T> implements SerializeFactory {

    /**
     * 由于 Kryo 不是线程安全的，并且构建和配置 Kryo 实例的成本相对较高，因此在多线程环境中可能会考虑使用 ThreadLocal 或池化。
     *
     * 具体可查看Github wiki：https://github.com/EsotericSoftware/kryo#input
     */
    static private final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // Configure the Kryo instance.
            return kryo;
        };
    };


    @Override
    public <T> byte[] serialize(T t) {
        Output output = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            output = new Output(byteArrayOutputStream);
            Kryo kryo = kryos.get();
            kryo.register(t.getClass());
            kryo.writeClassAndObject(output, t);
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        Input input = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            input = new Input(byteArrayInputStream);
            Kryo kryo = kryos.get();
            return kryo.readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

}
