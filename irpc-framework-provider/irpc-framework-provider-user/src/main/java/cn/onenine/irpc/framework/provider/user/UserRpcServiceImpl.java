package cn.onenine.irpc.framework.provider.user;

import cn.onenine.irpc.framework.interfaces.good.GoodRpcService;
import cn.onenine.irpc.framework.interfaces.pay.PayRpcService;
import cn.onenine.irpc.framework.interfaces.user.UserRpcService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcReference;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcService;

import java.util.*;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 20:06
 */
@IRpcService(group = "dev")
public class UserRpcServiceImpl implements UserRpcService {
    @IRpcReference
    private GoodRpcService goodRpcService;
    @IRpcReference
    private PayRpcService payRpcService;

    @Override
    public String getUserId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public List<Map<String, String>> findMyGoods(String userId) {
        List<String> goodsNoList = goodRpcService.selectGoodsNoByUserId(userId);
        List<Map<String, String>> finalResult = new ArrayList<>();
        goodsNoList.forEach(goodsNo -> {
            Map<String, String> item = new HashMap<>(2);
            List<String> payHistory = payRpcService.getPayHistoryByGoodNo(goodsNo);
            item.put(goodsNo, payHistory.toString());
            finalResult.add(item);
        });
        return finalResult;
    }
}
