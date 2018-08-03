package com.baojie.zk.example.okhttp;

import okhttp3.*;
import okio.BufferedSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class LearnOkHttp {
    private static final Logger log = LoggerFactory.getLogger(LearnOkHttp.class);
    private final ConnectionPool pool = new ConnectionPool(200, 1, TimeUnit.HOURS);

    private final OkHttpClient client = new OkHttpClient.Builder().connectionPool(pool).build();

    public LearnOkHttp() {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                client.dispatcher().executorService().shutdown();
                client.connectionPool().evictAll();
                try {
                    client.cache().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

    }

    public void asyncGet() {
        String url = "https://wwww.baidu.com";
        //OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .get()//默认就是GET请求，可以不写
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String tn = Thread.currentThread().getName();
                log.debug("tn=" + tn + ", call=" + call.toString() + ", excep=" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String n = response.header("Content-Disposition");

                String tn = Thread.currentThread().getName();
                log.debug("tn=" + tn + ", resp=" + response.body().string());
                response.close();
            }
        });
    }

    public void syncGet() {
        String url = "http://wwww.baidu.com";
        final Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = client.newCall(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = call.execute();
                    log.debug("run: " + response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void postString() {
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        String requestBody = "I am Jdqm.";
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(mediaType, requestBody))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.debug(response.protocol() + " " + response.code() + " " + response.message());
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    log.debug(headers.name(i) + ":" + headers.value(i));
                }
                log.debug("onResponse: " + response.body().string());
            }
        });
    }

    public void postStream() {
        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("text/x-markdown; charset=utf-8");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeUtf8("I am Jdqm.");
            }
        };

        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.debug(response.protocol() + " " + response.code() + " " + response.message());
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    log.debug(headers.name(i) + ":" + headers.value(i));
                }
                log.debug("onResponse: " + response.body().string());
            }
        });
    }

    private static final class TestRun implements Runnable {
        private final LearnOkHttp learn;

        public TestRun(LearnOkHttp learn) {
            this.learn = learn;
        }

        @Override
        public void run() {
            for (; ; ) {
                learn.asyncGet();
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(100, TimeUnit.MINUTES));
            }
        }

    }

    public static void main(String args[]) {
        LearnOkHttp learn = new LearnOkHttp();
        TestRun tr = null;
        for (int i = 0; i < 1; i++) {
            tr = new TestRun(learn);
            Thread t = new Thread(tr, "test_" + i);
            t.start();
        }
    }


}
