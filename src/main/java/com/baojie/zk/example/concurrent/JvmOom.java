package com.baojie.zk.example.concurrent;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class JvmOom {

    public static void main(String args[]){
        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },"t2");
        t2.start();
        Thread.yield();
//        Thread t1=new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final LinkedList list=new LinkedList();
//                while (true){
//                    list.add(new Object());
//                    LockSupport.parkNanos(1);
//                }
//            }
//        },"t1");
//        t1.start();
        LockSupport.parkNanos(1000);
//        try {
//            t2.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        throw new OutOfMemoryError();
        //System.out.println("main_exist");
    }
}
