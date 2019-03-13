package com.baojie.zk.example.netty.net;

import com.baojie.zk.example.concurrent.seda_refactor_01.bus.Bus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class HttpBus implements Bus {

    public String getBody(FullHttpRequest request) {
        ByteBuf buf = request.content();
        String body = buf.toString(CharsetUtil.UTF_8);
        return body;
    }

    public void send(ChannelHandlerContext ctx, String context, HttpResponseStatus status) {
        FullHttpResponse response = buildResp(context, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private FullHttpResponse buildResp(String con, HttpResponseStatus stat) {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, stat, Unpooled.copiedBuffer(con, CharsetUtil.UTF_8));
    }

}
