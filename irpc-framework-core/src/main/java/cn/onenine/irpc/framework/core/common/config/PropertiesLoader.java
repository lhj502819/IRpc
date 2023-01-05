package cn.onenine.irpc.framework.core.common.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Description：配置加载器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 20:47
 */
public class PropertiesLoader {

    private static Properties properties;

    private static Map<String, String> propertiesMap = new HashMap<>();

    private static String DEFAULT_PROPERTIES_FILE = "irpc.properties";

    public static void loadConfiguration() throws IOException {
        if (properties != null) {
            return;
        }

        properties = new Properties();
//        FileInputStream in = new FileInputStream(DEFAULT_PROPERTIES_FILE);
        InputStream in = PropertiesLoader.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE);
        properties.load(in);

    }

    public static String getPropertiesStr(String key) {
        if (properties == null || StrUtil.isBlank(key)) {
            return null;
        }

        if (!properties.containsKey(key)){
            throw new NullPointerException("key is null");
        }

        String value = properties.getProperty(key);

        if (!propertiesMap.containsKey(key)) {
            propertiesMap.put(key, value);
        }
        return value;
    }

    public static Integer getPropertiesInteger(String key) {
        if (properties == null || StrUtil.isBlank(key)) {
            return null;
        }

        if (!properties.containsKey(key)){
            throw new NullPointerException("key is null");
        }

        String value = properties.getProperty(key);

        if (!propertiesMap.containsKey(key)) {
            propertiesMap.put(key, value);
        }

        return Integer.valueOf(value);
    }



}
