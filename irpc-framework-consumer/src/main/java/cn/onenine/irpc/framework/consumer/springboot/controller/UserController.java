package cn.onenine.irpc.framework.consumer.springboot.controller;

import cn.onenine.irpc.framework.interfaces.DataService;
import cn.onenine.irpc.framework.springboot.starter.common.IRpcReference;
import com.google.common.collect.Lists;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/8 11:12
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @IRpcReference(group = "dev", serviceToken = "token-a",timeOut = 3000)
    private DataService dataService;

    @GetMapping("/query")
    public String query() {
        try {
            return dataService.sendData("get user info");
        }catch (Exception e){
            return "error";
        }
    }

    @GetMapping("/getList")
    public List<String> getList() {
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        try {
            return dataService.getList();
        }catch (Exception e){
            LOGGER.error("getList error", e);
            return Lists.newArrayList("error");
        }
    }

}
