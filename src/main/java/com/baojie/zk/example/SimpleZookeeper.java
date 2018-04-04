package com.baojie.zk.example;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.File;
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
//        ZooKeeper zookeeper = new ZooKeeper("127.0.0.1:2181", 5000, new SimpleZookeeper());
//        System.out.println(zookeeper.getState());
//        try {
//            connectedSemaphore.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Zookeeper session established");
        File f = new File("/home/baojie/liuxin/work/workspace/sms_add.txt");
        int i = 0;
        LineIterator iterator = FileUtils.lineIterator(f);
        String time="2018-03-16 18:03:24";
        String s0="INSERT INTO `t_flow_event_sms` (`content`, `date_created`, `event_name`, `last_updated`, `remark`)" +
                " VALUES (";

        String re="后台成功短信配置v3";

        while (iterator.hasNext()) {
            String s = iterator.nextLine();
            if(s==null){
                continue;
            }
            s=s.trim();
            int index=s.indexOf("尊");
            String s1=s.substring(index);
            //System.out.println(s1);

            int index0=s.indexOf("2-");

            String s3=s.substring(0,index0+10);
            //System.out.println(s3);
System.out.println(s0+"\""+s1+"\""+","+"\""+time+"\""+","+"\""+s3+"\""+","+"\""+time+"\""+","+"\""+re+"\""+");");




            //System.out.println(i + "=" + s);
            i++;
        }

        iterator.close();


    }
}