package cn.onenine.irpc.framework.core.spi.jdk;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/4 21:10
 */
public class DefaultISpiTest implements ISpiTest{
    @Override
    public void doSomething() {
        System.out.println("执行测试方法");
    }
}
