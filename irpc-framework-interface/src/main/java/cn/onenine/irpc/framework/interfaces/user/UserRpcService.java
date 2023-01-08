package cn.onenine.irpc.framework.interfaces.user;

import java.util.List;
import java.util.Map;

public interface UserRpcService {

    String getUserId();

    List<Map<String, String>> findMyGoods(String userId);
}
