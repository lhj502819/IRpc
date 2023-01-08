package cn.onenine.irpc.framework.consumer.springboot.controller;

import cn.onenine.irpc.framework.interfaces.good.GoodRpcService;
import cn.onenine.irpc.framework.interfaces.pay.PayRpcService;
import cn.onenine.irpc.framework.interfaces.user.UserRpcService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description：
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 20:32
 */
@RestController
@RequestMapping("test")
public class ApiTestController {

    @IRpcReference(group = "dev")
    private UserRpcService userRpcService;
    @IRpcReference(group = "dev")
    private GoodRpcService goodRpcService;
    @IRpcReference(group = "dev")
    private PayRpcService payRpcService;

    @GetMapping(value = "/do-test")
    public boolean doTest() {
        long begin1 = System.currentTimeMillis();
        userRpcService.getUserId();
        long end1 = System.currentTimeMillis();
        System.out.println("userRpc--->" + (end1 - begin1) + "ms");
        long begin2 = System.currentTimeMillis();
        goodRpcService.decreaseStock();
        long end2 = System.currentTimeMillis();
        System.out.println("goodRpc--->" + (end2 - begin2) + "ms");
        long begin3 = System.currentTimeMillis();
        payRpcService.doPay();
        long end3 = System.currentTimeMillis();
        System.out.println("payRpc--->" + (end3 - begin3) + "ms");
        return true;
    }


    @GetMapping(value = "/do-test-2")
    public void doTest2() {
        String userId = userRpcService.getUserId();
        System.out.println("userRpcService result: " + userId);
        boolean goodResult = goodRpcService.decreaseStock();
        System.out.println("goodRpcService result: " + goodResult);
    }

}
