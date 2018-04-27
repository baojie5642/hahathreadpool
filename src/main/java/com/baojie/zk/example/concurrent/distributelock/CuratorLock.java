package com.baojie.zk.example.concurrent.distributelock;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.TimeUnit;

public class CuratorLock {

    private final int CONNECT_TIMEOUT = 15000;
    private final int RETRY_TIME = Integer.MAX_VALUE;
    private final int RETRY_INTERVAL = 1000;


    public static void main(String args[]){
         final int CONNECT_TIMEOUT = 15000;
         final int RETRY_TIME = Integer.MAX_VALUE;
         final int RETRY_INTERVAL = 1000;
        CuratorFramework curator = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .retryPolicy(new RetryNTimes(RETRY_TIME, RETRY_INTERVAL))
                .connectionTimeoutMs(CONNECT_TIMEOUT).build();

        curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            public void stateChanged(CuratorFramework client, ConnectionState state) {
                if (state == ConnectionState.LOST) {
                    //连接丢失
                    System.out.println("lost session with zookeeper");
                } else if (state == ConnectionState.CONNECTED) {
                    //连接新建
                    System.out.println("connected with zookeeper");
                } else if (state == ConnectionState.RECONNECTED) {
                    System.out.println("reconnected with zookeeper");
                    //连接重连

                }
            }
        });
        curator.start();


        InterProcessMutex lock = new InterProcessMutex(curator, "/test0");
        try {
            if ( lock.acquire(3, TimeUnit.SECONDS) )
            {
                try
                {
                    System.out.println("get lock");
                }
                finally
                {
                    try {
                        lock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        curator.close();
    }






}
