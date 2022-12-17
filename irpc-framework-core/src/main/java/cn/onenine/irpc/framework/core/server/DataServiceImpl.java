package cn.onenine.irpc.framework.core.server;

import cn.onenine.irpc.framework.interfaces.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 14:14
 */
public class DataServiceImpl implements DataService {

    private Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);

    public String sendData(String body) {
        logger.info("已收到的参数长度：" + body.length());
        return "success";
    }

    public List<String> getList() {
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("nine 9");
        arrayList.add("nine 9");
        arrayList.add("nine 9");
        return arrayList;
    }
}
