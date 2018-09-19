package com.baojie.zk.example.netty.local;

public class OpenServer {

    private static void startLocalServer() throws InterruptedException {
        LocalServer server = new LocalServer("hello");
        server.start();
    }

    public static void main(String[] args) throws InterruptedException {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLocalServer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();

        Thread.sleep(1000);

    }
}
