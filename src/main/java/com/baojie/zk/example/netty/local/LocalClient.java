package com.baojie.zk.example.netty.local;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;

public class LocalClient {

    private String remoteAddress;
    public LocalClient(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
    public void start() {
        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup);
            b.channel(LocalChannel.class);
            b.handler(new ChannelInitializer<LocalChannel>() {
                @Override protected void initChannel(LocalChannel ch) throws Exception {
                    ch.pipeline().addLast(new LocalClientHandler());
                }
            });
            LocalAddress address = new LocalAddress(this.remoteAddress);
            ChannelFuture future = b.connect(address).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            System.out.println("error !" + e);
        }
    }
}
