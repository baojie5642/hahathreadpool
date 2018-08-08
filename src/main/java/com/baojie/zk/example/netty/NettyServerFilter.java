package com.baojie.zk.example.netty;

import com.baojie.zk.example.concurrent.seda_refactor_01.Stage;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;


public class NettyServerFilter extends ChannelInitializer<SocketChannel> {

    private final Stage<HttpBus> s;

    public NettyServerFilter(Stage<HttpBus> s) {
        this.s = s;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline ph = ch.pipeline();
        //处理http服务的关键handler
        ph.addLast("encoder", new HttpResponseEncoder());
        ph.addLast("decoder", new HttpRequestDecoder());
        ph.addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
        ph.addLast("compress", new HttpContentCompressor());
        ph.addLast("handler", new NettyServerHandler(s));// 服务端业务逻辑
    }
}
