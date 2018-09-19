package com.baojie.zk.example.netty.local;

public class OpenClient {


    private static void startLocalClient() {
        LocalClient client = new LocalClient("hello");
        client.start();
    }

    public static void main(String[] args) throws InterruptedException {
        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startLocalClient();
            }
        });
        clientThread.start();
    }
}
