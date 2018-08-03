package com.baojie.zk.example.okhttp;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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


    private final String url = "https://www.baidu.com/";

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

        File file = new File("/home/baojie/liuxin/work/test.txt");
        try {
            readString(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }




    }

}
