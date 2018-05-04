package com.baojie.zk.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleZookeeper implements Watcher {
    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    @Override
    public void process(WatchedEvent event) {
        System.out.println("Receive watched event : " + event);
        if (Event.KeeperState.SyncConnected == event.getState()) {
            connectedSemaphore.countDown();
        }
    }

    public static void main(String[] args) throws Exception {
//        ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 1000, new SimpleZookeeper());
//        String path = "/test";
//        System.out.println(zk.getState());
//        try {
//            connectedSemaphore.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        zk.create(path, "123".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
//        System.out.println("success create znode: " + path);
//        zk.getData(path, true, null);
//
//        Stat stat = zk.setData(path, "456".getBytes(), -1);
//        System.out.println(
//                "czxID: " + stat.getCzxid() + ", mzxID: " + stat.getMzxid() + ", version: " + stat.getVersion());
//        Stat stat2 = zk.setData(path, "456".getBytes(), stat.getVersion());
//        System.out.println(
//                "czxID: " + stat2.getCzxid() + ", mzxID: " + stat2.getMzxid() + ", version: " + stat2.getVersion());
//        try {
//            zk.setData(path, "456".getBytes(), stat.getVersion());
//        } catch (KeeperException e) {
//            System.out.println("Error: " + e.code() + "," + e.getMessage());
//        }
        final ReentrantLock lock = new ReentrantLock();


        Runnable r0 = new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    try {
                        TimeUnit.SECONDS.sleep(3600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread t0 = new Thread(r0, "r_0_test");
        t0.start();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runnable r1 = new Runnable() {
            @Override
            public void run() {

                try {

                    lock.lockInterruptibly();

                    try {
                        TimeUnit.SECONDS.sleep(3600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        };
        Thread t1 = new Thread(r1, "r_1_test");
        t1.start();

        try {
            TimeUnit.SECONDS.sleep(16);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        t1.interrupt();


    }
}