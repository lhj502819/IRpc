package cn.onenine.irpc.framework.core.spi.jdk;

import java.sql.DriverManager;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @since 2023/1/4 21:12
 */
public class TestSpiDemo {

    public static void doTest(ISpiTest spiTest){
        System.out.println("begin");
        spiTest.doSomething();
        System.out.println("end");
    }

    public static void main(String[] args) {
        ServiceLoader<ISpiTest> serviceLoader = ServiceLoader.load(ISpiTest.class);
        Iterator<ISpiTest> iSpiTestIterator = serviceLoader.iterator();
        while (iSpiTestIterator.hasNext()) {
            ISpiTest iSpiTest = iSpiTestIterator.next();
            TestSpiDemo.doTest(iSpiTest);
        }
    }


}
