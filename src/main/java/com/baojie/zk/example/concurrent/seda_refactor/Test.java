package com.baojie.zk.example.concurrent.seda_refactor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Test {
    public static void main(String args[]) {
        Stage_Business bus = new Stage_Business();
        Stage_Refactor sr = new Stage_Refactor(3, 6, 15, TimeUnit.SECONDS, "test", bus);
        sr.prestartCoreThread();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        for (int i = 0; i < 5; i++) {
            Stage_Task task = new Stage_Task() {
                @Override
                public void task(Stage_Business bus) {
                    bus.bus();
                }
            };
            if (sr.submit(task)) {
                System.out.println("ok");
            } else {
                System.out.println("no");
            }
        }
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30));
        sr.shutdown();
        System.out.println("shutDown");
        System.out.println(sr.isShutdown());
        System.out.println(sr.isTerminated());
        System.out.println(sr.isTerminating());
    }

}
