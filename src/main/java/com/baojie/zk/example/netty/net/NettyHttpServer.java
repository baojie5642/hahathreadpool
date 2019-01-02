package com.baojie.zk.example.netty.net;

import com.baojie.zk.example.concurrent.seda_refactor_01.Stage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyHttpServer {

    static final boolean enableEpoll;

    static {

        enableEpoll = Epoll.isAvailable();

    }


    private static final int port = 6789; //设置服务端端口
    private final EventLoopGroup bossGroup;   // 通过nio方式来接收连接和处理连接
    private final EventLoopGroup workerGroup;   // 通过nio方式来接收连接和处理连接
    private final ServerBootstrap b = new ServerBootstrap();
    private final HttpBus hb = new HttpBus();
    private final Stage<HttpBus> stage = new Stage<HttpBus>(1, 1, 180, "http_stage", hb);

    public NettyHttpServer() {
        if (enableEpoll) {
            this.bossGroup = new EpollEventLoopGroup();
        } else {
            this.bossGroup = new NioEventLoopGroup();
        }

        if (enableEpoll) {
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            this.workerGroup = new NioEventLoopGroup();
        }
    }


    public void start() {
        try {
            b.group(bossGroup, workerGroup);
            if (enableEpoll) {
                b.channel(EpollServerSocketChannel.class);
            } else {
                b.channel(NioServerSocketChannel.class);
            }
            b.childHandler(new NettyServerFilter(stage));
            b.option(ChannelOption.SO_BACKLOG, 128);
            b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
            bind();
        } finally {
            bossGroup.shutdownGracefully(); //关闭EventLoopGroup，释放掉所有资源包括创建的线程
            workerGroup.shutdownGracefully(); //关闭EventLoopGroup，释放掉所有资源包括创建的线程
        }
    }

    private void bind() {
        ChannelFuture f = null;
        try {
            f = b.bind(port).sync();
            System.out.println("服务端启动成功,端口是:" + port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        listen(f);
    }

    private void listen(ChannelFuture f) {
        try {
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NettyHttpServer server = new NettyHttpServer();
        server.start();
    }
}
