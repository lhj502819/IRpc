package cn.onenine.irpc.framework.provider.pay;

import cn.onenine.irpc.framework.interfaces.good.GoodRpcService;
import cn.onenine.irpc.framework.interfaces.pay.PayRpcService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcService;

import java.util.Arrays;
import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 20:06
 */
@IRpcService(group = "dev")
public class PayRpcServiceImpl implements PayRpcService {

    @Override
    public boolean doPay() {
        return true;
    }

    @Override
    public List<String> getPayHistoryByGoodNo(String goodNo) {
        System.out.println("getPayHistoryByGoodNo");
        return Arrays.asList(goodNo + "-pay-001", goodNo + "-pay-002");
    }
}
