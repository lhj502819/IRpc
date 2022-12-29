package cn.onenine.irpc.framework.core.common.event;

import cn.hutool.core.collection.CollectionUtil;
import cn.onenine.irpc.framework.core.common.event.listener.IRpcListener;
import cn.onenine.irpc.framework.core.common.event.listener.ProviderNodeDataChangeListener;
import cn.onenine.irpc.framework.core.common.event.listener.ServiceDestroyListener;
import cn.onenine.irpc.framework.core.common.event.listener.ServiceUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description：事件上下文类，用于发送事件
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 14:06
 */
public class IRpcListenerLoader {

    private static Logger logger = LoggerFactory.getLogger(IRpcListenerLoader.class);

    private static List<IRpcListener> iRpcListeners = new ArrayList<>();

    private static ExecutorService eventThreadPool = Executors.newFixedThreadPool(2);


    public static void registerListener(IRpcListener<?> listener) {
        iRpcListeners.add(listener);
    }

    public void init() {
        registerListener(new ServiceUpdateListener());
        registerListener(new ProviderNodeDataChangeListener());
        registerListener(new ServiceDestroyListener());
    }

    /**
     * 获取泛型 T
     */
    public static Class<?> getInterfaceT(Object o) {
        Type[] types = o.getClass().getGenericInterfaces();
        ParameterizedType parameterizedTypes = (ParameterizedType) types[0];
        Type type = parameterizedTypes.getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return null;
    }

    public static void sendEvent(final IRpcEvent iRpcEvent) {
        if (CollectionUtil.isEmpty(iRpcListeners)) {
            return;
        }

        for (final IRpcListener iRpcListener : iRpcListeners) {
            //判断Class的泛型
            Class<?> type = getInterfaceT(iRpcListener);
            if (type.equals(iRpcEvent.getClass())) {
                //是当前listener监听的事件类型
                eventThreadPool.execute(() -> {
                    try {
                        iRpcListener.callBack(iRpcEvent.getData());
                    } catch (Exception e) {
                        logger.error("sendEvent error", e);
                    }
                });
            }
        }
    }

    public static void sendSyncEvent(final IRpcEvent iRpcEvent) {
        if (CollectionUtil.isEmpty(iRpcListeners)) {
            return;
        }

        for (final IRpcListener iRpcListener : iRpcListeners) {
            //判断Class的泛型
            Class<?> type = getInterfaceT(iRpcListener);
            if (type.equals(iRpcEvent.getClass())) {
                //是当前listener监听的事件类型
                try {
                    iRpcListener.callBack(iRpcEvent.getData());
                } catch (Exception e) {
                    logger.error("sendEvent error", e);
                }
            }
        }
    }


}
