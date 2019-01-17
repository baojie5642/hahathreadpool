package com.baojie.zk.example.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class NIOServer {
    private static int BUFF_SIZE = 1024;
    private static int TIME_OUT = 2000;

    private static final int BUG_TIMES = 512;

    public static void main(String[] args) throws IOException {

        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(10083));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        TCPProtocol protocol = new EchoSelectorProtocol(BUFF_SIZE);
        int bugs = 0;
        while (true) {
            try {
                long ss = System.nanoTime();
                if (selector.select(TIME_OUT) == 0) {
                    long es = System.nanoTime();
                    if (es - ss >= TimeUnit.MILLISECONDS.toNanos(TIME_OUT)) {
                        //在等待信道准备的同时，也可以异步地执行其他任务，  这里打印*
                        bugs = 1;
                        System.out.print("*");
                    } else {
                        Thread.yield();
                        bugs++;
                        if (bugs >= BUG_TIMES) {
                            Selector old = selector;
                            Selector newSele = Selector.open();
                            for (SelectionKey key : old.keys()) {
                                Object a = key.attachment();
                                try {
                                    if (!key.isValid()) {
                                        continue;
                                    }
                                    // 如果不为null，说明有channel已经注册到这个新的信道上面了
                                    SelectionKey test = key.channel().keyFor(newSele);
                                    if (null != test) {
                                        continue;
                                    }
                                    int interestOps = key.interestOps();
                                    key.cancel();
                                    // 将原来的信道注册到新的key上面
                                    SelectionKey newKey = key.channel().register(newSele, interestOps, a);
                                    bugs = 1;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            selector = newSele;
                            selector.selectNow();
                            try {
                                // time to close the old selector as everything else is registered to the new one
                                old.close();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            bugs = 1;
                        } else {
                            System.out.print("*");
                        }
                    }
                }
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    //如果服务端信道感兴趣的I/O操作为accept
                    if (key.isAcceptable()) {
                        protocol.handleAccept(key);
                    }
                    //如果客户端信道感兴趣的I/O操作为read
                    if (key.isReadable()) {
                        protocol.handleRead(key);
                    }
                    //如果该键值有效，并且其对应的客户端信道感兴趣的I/O操作为write
                    if (key.isValid() && key.isWritable()) {
                        protocol.handleWrite(key);
                    }

                    //这里需要手动从键集中移除当前的key
                    keyIter.remove();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
