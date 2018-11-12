package com.baojie.zk.example.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockDetec {
    public static void main(String args[]) {
        final StringBuilder sbu = new StringBuilder(4);
        sbu.append("init");
        final ReentrantLock lock = new ReentrantLock();
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(sbu.toString());
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.lock();
                try {
                    sbu.append("thread_1_append");
                } finally {
                    lock.unlock();
                }
                System.out.println(sbu.toString());
            }
        }, "t1");
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(sbu.toString());
                try {
                    TimeUnit.SECONDS.sleep(6);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(sbu.toString());
                lock.lock();
                try {
                    System.out.println(sbu.toString());
                } finally {
                    lock.unlock();
                }
            }
        }, "t2");
        t1.start();
        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();
    }
}
