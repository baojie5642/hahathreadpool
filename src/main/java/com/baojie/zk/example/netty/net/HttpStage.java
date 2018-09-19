package com.baojie.zk.example.netty.net;

import com.baojie.zk.example.concurrent.seda_refactor_01.Bus;
import com.baojie.zk.example.concurrent.seda_refactor_01.StageTask;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;

public class HttpStage implements StageTask {

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest httpReq;

    public HttpStage(ChannelHandlerContext ctx, FullHttpRequest httpReq) {
        this.ctx = ctx;
        this.httpReq = httpReq;
    }

    @Override
    public void task(Bus bus) {
        if (null == bus) {
            return;
        }
        if (!(bus instanceof HttpBus)) {
            return;
        }
        HttpBus hb = (HttpBus) bus;
        try {
            String path = httpReq.uri();          //获取路径
            String body = hb.getBody(httpReq);     //获取参数
            HttpMethod method = httpReq.method();//获取请求方法
            if (!"/test".equalsIgnoreCase(path)) {
                hb.send(ctx, "非法请求!", HttpResponseStatus.BAD_REQUEST);
                return;
            }
            if (HttpMethod.GET.equals(method)) {
                hb.send(ctx, "GET请求", HttpResponseStatus.OK);
                return;
            }
            if (HttpMethod.POST.equals(method)) {
                hb.send(ctx, "POST请求", HttpResponseStatus.OK);
                return;
            }
            if (HttpMethod.PUT.equals(method)) {
                hb.send(ctx, "PUT请求", HttpResponseStatus.OK);
                return;
            }
            if (HttpMethod.DELETE.equals(method)) {
                hb.send(ctx, "DELETE请求", HttpResponseStatus.OK);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }

    }

    private void release() {
        try {
            httpReq.release();
        } finally {
            ReferenceCountUtil.safeRelease(httpReq);
        }
    }

}
