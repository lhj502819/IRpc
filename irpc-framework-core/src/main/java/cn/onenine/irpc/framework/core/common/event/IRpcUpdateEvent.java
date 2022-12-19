package cn.onenine.irpc.framework.core.common.event;

/**
 * Description：节点更新事件
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/19 14:00
 */
public class IRpcUpdateEvent implements IRpcEvent{

    private Object data;

    public IRpcUpdateEvent(Object data) {
        this.data = data;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public IRpcEvent sendData(Object data) {
        this.data = data;
        return this;
    }
}
