package com.baojie.zk.example.okhttp;

import com.baojie.zk.example.okhttp.util_00.CallBackUtil;
import com.baojie.zk.example.okhttp.util_00.OkhttpUtil;
import okhttp3.*;
import okio.BufferedSource;
import okio.Okio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class HttpOKTest {

    private final OkHttpClient okHttpClient;
    private final OkHttpClient.Builder clientb = new OkHttpClient.Builder();

    public HttpOKTest() {
        this.clientb.connectTimeout(3600, TimeUnit.SECONDS);
        this.clientb.readTimeout(3600, TimeUnit.SECONDS);
        this.clientb.writeTimeout(3600, TimeUnit.SECONDS);
        this.okHttpClient = clientb.build();
    }

    public static void readString(InputStream in) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(in));  //创建BufferedSource
        String s = source.readUtf8();  //以UTF-8读
        System.out.println(s);     //打印
        source.close();
    }


    private final String url = "http://127.0.0.1:9123/test";

    public void test() {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        Callback cb = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Response temp=response;
                System.out.println(response);

                System.out.println("我是异步线程,线程Id为:" + Thread.currentThread().getId() + response);
            }
        };
        call.enqueue(cb);
        for (int i = 0; i < 10; i++) {
            System.out.println("我是主线程,线程Id为:" + Thread.currentThread().getId());
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String args[]) {
        HttpOKTest t = new HttpOKTest();
        //t.test();


        OkhttpUtil.okHttpPost("http://127.0.0.1:9123/test", new CallBackUtil() {
            @Override
            public Object onParseResponse(Call call, Response response) {
                try {
                    String resp= response.body().string();
                    System.out.println(resp);

                    return resp;
                } catch (IOException e) {
                    new RuntimeException("failure");
                    return "";
                }
            }

            @Override
            public void onFailure(Call call, Exception e) {
                System.out.println("fail");
            }

            @Override
            public void onResponse(Object response) {
                try {
                    System.out.println(response);


                } catch (Throwable e) {
                    new RuntimeException("failure");

                }
            }
        });


        //File file = new File("/home/baojie/liuxin/work/test.txt");
        //try {
        //    readString(new FileInputStream(file));
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}




    }

}
