package com.baojie.zk.example.concurrent.locktest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Test4Lock {

    public static void main(String args[]){
        final ReentrantLock mainLock=new ReentrantLock();

        Thread t0=new Thread(new Runnable() {
            @Override
            public void run() {
                mainLock.lock();
                try {
                    try {
                        TimeUnit.SECONDS.sleep(60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }finally {
                    mainLock.unlock();
                }


            }
        },"t_0");

        t0.start();
        Thread.yield();
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(36);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mainLock.lock();
                try {
                    try {
                        TimeUnit.SECONDS.sleep(36);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }finally {
                    mainLock.unlock();
                }


            }
        },"t_1");
        t1.start();
        Thread.yield();
        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(18);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mainLock.lock();
                try {
                    try {
                        TimeUnit.SECONDS.sleep(18);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }finally {
                    mainLock.unlock();
                }


            }
        },"t_2");
        t2.start();
        Thread.yield();
    }

}
