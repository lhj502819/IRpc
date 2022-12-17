package cn.onenine.irpc.framework.interfaces;

import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 14:14
 */
public interface DataService {

    /**
     * 发送数据
     */
    String sendData(String body);

    /**
     * 获取数据
     */
    List<String> getList();


}
