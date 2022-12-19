package cn.onenine.irpc.framework.core.common.event;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 13:59
 */
public interface IRpcEvent {

    Object getData();

    IRpcEvent sendData(Object data);

}
