package cn.onenine.irpc.framework.core.common.event;

/**
 * Description：服务销毁事件
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/29 17:17
 */
public class IRpcDestroyEvent implements IRpcEvent{

    private Object data;

    public IRpcDestroyEvent(Object data) {
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
