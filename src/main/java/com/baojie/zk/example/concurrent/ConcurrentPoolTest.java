package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ConcurrentPoolTest implements Runnable {
    private final Logger log = LoggerFactory.getLogger(ConcurrentPoolTest.class);

    private final String name;

    private ConcurrentPoolTest(String name) {
        this.name = name;
    }

    public static ConcurrentPoolTest create(String name) {
        return new ConcurrentPoolTest(name);
    }

    @Override
    public void run() {
        int i = 0;
        for (; ; ) {
            i++;
            try {
                TimeUnit.MICROSECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.error("name=" + name + ", has print=" + i + "， 刘康博——2018.02.14……");
            if (i == 26000) {
                log.error("name=" + name + ", has print=" + i + ", break");
                break;
            }
        }
    }

    // error in GitHub, fixing
    // 过年回来git坏了，不知道什么问题，http://blog.csdn.net/ykttt1/article/details/47292821

    public static void main(String args[]) {
        Thread tttt = Thread.currentThread();
        LockSupport.unpark(tttt);
        System.out.println("unpark");
        // 不管是哪种类型，成对出现就可以了
        LockSupport.parkNanos(tttt, 999999999999L);
        System.out.println("unpark____over");

        // 之后的测试采用触发式监控文件内容的形式，文件监控器已经写好了
        // 后续跟进
        ConcurrentPool pool = new ConcurrentPool(1, 4, 10, TimeUnit.SECONDS, TFactory.create
                ("concurrent_test"));
        ConcurrentPoolTest t = null;
        for (int i = 0; i < 3; i++) {
            t = ConcurrentPoolTest.create("baojie_" + i);
            pool.submit(t);
        }

        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.MINUTES));
        t = ConcurrentPoolTest.create("baojie_Other");
        pool.submit(t);

        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(2, TimeUnit.MINUTES));
        pool.shutdown();
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(2, TimeUnit.MINUTES));
        pool.shutdownNow();

    }

}
