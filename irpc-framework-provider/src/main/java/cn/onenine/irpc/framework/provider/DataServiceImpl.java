package cn.onenine.irpc.framework.provider;

import cn.onenine.irpc.framework.interfaces.DataService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/17 14:14
 */
@IRpcService(group = "dev",limit = 10,serviceToken = "token-b")
public class DataServiceImpl implements DataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataServiceImpl.class);

    @Override
    public String sendData(String body) {
        LOGGER.info("已收到的参数长度：" + body.length());
        int i = 1/0;
        return "success";
    }

    @Override
    public List<String> getList() {
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("nine 9");
        arrayList.add("nine 9");
        arrayList.add("nine 9");
        return arrayList;
    }
}
