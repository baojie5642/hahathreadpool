package com.baojie.zk.example.concurrent;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class PoolTest {

    private final BaojieThreadPool pool = new BaojieThreadPool(1, 3, 180, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(30)
            , TFactory
            .create("baojie_pool")) {

        public void beforeExecute(Thread t, Runnable r) {
            System.out.println(t.getName() + ",beforeExecute test in");
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
            System.out.println(t.getName() + ",beforeExecute test out");
        }

        protected void afterExecute(Runnable r, Throwable t) {
            if(null!=t){
                System.out.println(t.toString() + ",afterExecute test in");
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
                System.out.println(t.toString() + ",afterExecute test out");
                //throw new Error("baojie test in afterExecute");
            }
        }


    };

    public PoolTest() {

    }

    public void startTest() {
        BaojieTest test = new BaojieTest();
        pool.submit(test);
    }

    private class BaojieTest implements Runnable {

        public BaojieTest() {

        }

        @Override
        public void run() {

            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(6, TimeUnit.SECONDS));
            double i = 12 / 0;// 抛出异常的地方
            System.out.println(i);
        }
    }


    public static void main(String args[]) {
        PoolTest test = new PoolTest();
        test.startTest();

    }


}
