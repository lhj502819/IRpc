package cn.onenine.irpc.framework.core.common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对一次调用的封装
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 10:53
 */
public class RpcInvocation implements Serializable {

    private static final long serialVersionUID = 4925694661803675105L;
    /**
     * 请求的目标方法，例如finderUser
     */
    private String targetMethod;

    /**
     * 请求的目标服务名称，例如：cn.onenine.user.UserService
     */
    private String targetServiceName;

    /**
     * 请求参数信息
     */
    private Object[] args;

    /**
     * 用于匹配请求和响应的一个关键值，当请求从客户端发出的时候，会有一个uuid用于记录发出的请求
     *  待数据返回的时候通过uuid来匹配对应的请求线程，并且返回给调用线程
     */
    private String uuid;

    /**
     * 接口响应的数据塞入这个字段中(如果是异步调用或者是void类型，这里就为空)
     */
    private Object response;

    /**
     * 异常堆栈
     */
    private Throwable e;

    /**
     * 重试机制
     */
    private int retry;
    /**
     * 附加信息
     */
    private Map<String, Object> attachments = new ConcurrentHashMap<>();

    public Throwable getE() {
        return e;
    }

    public void setE(Throwable e) {
        this.e = e;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getTargetServiceName() {
        return targetServiceName;
    }

    public void setTargetServiceName(String targetServiceName) {
        this.targetServiceName = targetServiceName;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public void clearRespAndError(){
        this.response = null;
        this.e = null;
    }

    @Override
    public String toString() {
        return "RpcInvocation{" +
                "targetMethod='" + targetMethod + '\'' +
                ", targetServiceName='" + targetServiceName + '\'' +
                ", args=" + Arrays.toString(args) +
                ", uuid='" + uuid + '\'' +
                ", response=" + response +
                '}';
    }
}
