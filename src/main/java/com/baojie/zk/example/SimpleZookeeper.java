package com.baojie.zk.example;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class SimpleZookeeper implements Watcher {
    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    @Override
    public void process(WatchedEvent event) {
        System.out.println("Receive watched event : " + event);
        if (Event.KeeperState.SyncConnected == event.getState()) {
            connectedSemaphore.countDown();
        }
    }

    public static void main(String[] args) throws IOException {
        ZooKeeper zookeeper = new ZooKeeper("127.0.0.1:2181", 5000, new SimpleZookeeper());
        System.out.println(zookeeper.getState());
        try {
            connectedSemaphore.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Zookeeper session established");
    }
}