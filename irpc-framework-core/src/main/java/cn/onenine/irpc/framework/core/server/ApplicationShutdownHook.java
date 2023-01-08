package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.core.common.event.IRpcDestroyEvent;
import cn.onenine.irpc.framework.core.common.event.IRpcListenerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description：监听Java进程被关闭
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/29 17:23
 */
public class ApplicationShutdownHook {

    public static Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);

    public static void registryShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(()->{

            IRpcListenerLoader.sendSyncEvent(new IRpcDestroyEvent("application destroy"));;
        }));
    }

}
