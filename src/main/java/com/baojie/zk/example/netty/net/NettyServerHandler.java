package com.baojie.zk.example.netty.net;

import com.baojie.zk.example.concurrent.seda_refactor_01.Stage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private final Stage<HttpBus> s;

    public NettyServerHandler(Stage<HttpBus> s) {
        this.s = s;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            s.getBus().send(ctx, "未知请求!", HttpResponseStatus.BAD_REQUEST);
        } else {
            FullHttpRequest req = (FullHttpRequest) msg;
            HttpStage hs = new HttpStage(ctx, req);
            if (HttpUtil.isKeepAlive(req)) {
                // netty中for循环处理请求，
                boolean sr=s.submit(hs);
                if (sr) {
                    return;
                } else {
                    hs.task(s.getBus());
                }
            } else {
                hs.task(s.getBus());
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel c = ctx.channel();
        if (null != c) {
            c.close();
        }
    }

}
