package com.baojie.zk.example.okhttp;

import okhttp3.*;

import java.io.IOException;

public class OkHttpClientBao {


    public static void main(String args[]){

        String url = "http://localhost:9123/actuator/refresh";
        OkHttpClient okHttpClient = new OkHttpClient();

        RequestBody body = new FormBody.Builder()
                .add("", "")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }



    }


}
