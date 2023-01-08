package cn.onenine.irpc.framework.provider.good;

import cn.onenine.irpc.framework.interfaces.good.GoodRpcService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcService;

import java.util.Arrays;
import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 20:06
 */
@IRpcService(group = "dev")
public class GoodRpcServiceImpl implements GoodRpcService {
    @Override
    public boolean decreaseStock() {
        return true;
    }

    @Override
    public List<String> selectGoodsNoByUserId(String userId) {
        return Arrays.asList(userId + "-good-01", userId + "-good-02");
    }
}
