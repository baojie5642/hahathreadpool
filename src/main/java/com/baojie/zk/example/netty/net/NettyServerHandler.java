package com.baojie.zk.example.netty.net;

import com.baojie.zk.example.concurrent.seda_refactor_01.Stage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS));
            //FullHttpRequest req = (FullHttpRequest) msg;
            //HttpStage hs = new HttpStage(ctx, req);
            //if (HttpUtil.isKeepAlive(req)) {
            // netty中for循环处理请求，
            //    boolean sr = s.submit(hs);
            //    if (sr) {
            //        return;
            //    } else {
            //        hs.task(s.getBus());
            //    }
            //} else {
            //    hs.task(s.getBus());
            //}
            FullHttpRequest httpReq=(FullHttpRequest)msg;
            try {
                String path = httpReq.uri();          //获取路径
                String body = getBody(httpReq);     //获取参数
                HttpMethod method = httpReq.method();//获取请求方法
                if (!"/test".equalsIgnoreCase(path)) {
                    send(ctx, "非法请求!", HttpResponseStatus.BAD_REQUEST);
                    return;
                }
                if (HttpMethod.GET.equals(method)) {
                    send(ctx, "GET请求", HttpResponseStatus.OK);
                    return;
                }
                if (HttpMethod.POST.equals(method)) {
                    send(ctx, "POST请求", HttpResponseStatus.OK);
                    return;
                }
                if (HttpMethod.PUT.equals(method)) {
                    send(ctx, "PUT请求", HttpResponseStatus.OK);
                    return;
                }
                if (HttpMethod.DELETE.equals(method)) {
                    send(ctx, "DELETE请求", HttpResponseStatus.OK);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                release(httpReq);
            }

        }
    }
        private void release(FullHttpRequest httpReq) {
            try {
                httpReq.release();
            } finally {

            }
        }


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
