package com.baojie.zk.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        String json = "{" +
                "\"" + "mobileNum" + "\"" + ":" + "\"" + "176" + "\"" + "," +
                "\"" + "guestMobileNum" + "\"" + ":" + "\"" + "176" + "\"" + "," +
                "\"" + "orderType" + "\"" + ":" + "\"" + "1" + "\"" + "," +
                "\"" + "pkgId" + "\"" + ":" + "\"" + "300" + "\"" + "," +
                "\"" + "userCouponId" + "\"" + ":" + "\"" + "" + "\"" + "," +
                "\"" + "activId" + "\"" + ":" + "\"" + "" + "\"" + "," +
                "\"" + "source" + "\"" + ":" + "\"" + "ill" + "\"" + "," +
                "\"" + "channelId" + "\"" + ":" + "\"" + "WAP" + "\"" + "," +
                "\"" + "channelNo" + "\"" + ":" + "\"" + "SHCT_WAP" + "\"" + "," +
                "\"" + "notifyUrl" + "\"" + ":" + "\"" + "" + "\"" + "," +
                "\"" + "payChannel" + "\"" + ":" + "\"" + "3" + "\"" + "," +
                "\"" + "forceCRM" + "\"" + ":" + "\"" + "false" + "\"" +
                "}";

String ssss="{\"mobileNum\":\"17301624025\",\"guestMobileNum\":\"17301624025\",\"orderType\":\"1\",\"pkgId\":\"300\"," +
        "\"userCouponId\":\"\",\"activId\":\"\",\"source\":\"ill\",\"channelId\":\"WAP\",\"channelNo\":\"SHCT_WAP\"," +
        "\"notifyUrl\":\"\",\"payChannel\":\"3\",\"forceCRM\":\"false\"}";

int llll=ssss.length();

System.out.println("lllllll="+llll);

        System.out.println("json=" + json);


    }
}