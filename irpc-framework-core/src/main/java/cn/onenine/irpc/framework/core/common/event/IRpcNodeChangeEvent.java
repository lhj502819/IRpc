package cn.onenine.irpc.framework.core.common.event;

/**
 * Description：节点信息变更事件
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/28 22:09
 */
public class IRpcNodeChangeEvent implements IRpcEvent{

    private Object data;

    public IRpcNodeChangeEvent(Object data) {
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
