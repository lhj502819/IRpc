package cn.onenine.irpc.framework.provider;

import cn.onenine.irpc.framework.interfaces.UserService;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2022/12/29 17:44
 */
public class UserServiceImpl implements UserService {
    @Override
    public void test() {
        System.out.println("test");
    }
}
