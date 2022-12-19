package cn.onenine.irpc.framework.core.common.event.listener;

/**
 * Description：Rpc事件监听器
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 14:01
 */
public interface IRpcListener<T> {

    void callBack(Object t);

}
