package com.baojie.zk.example.okhttp;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.GzipSource;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;

public class NeoHttpClient {

    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String MEDIA_TYPE_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MEDIA_TYPE_MULTIPART = "multipart/form-data";

    private static final int CONN_TIMEOUT = 15;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;

    private static Map<String, MediaType> mediaTypeMap = new HashMap<>();

    static {
        mediaTypeMap.put(MEDIA_TYPE_JSON, MediaType.parse(MEDIA_TYPE_JSON));
        mediaTypeMap.put(MEDIA_TYPE_URLENCODED, MediaType.parse(MEDIA_TYPE_URLENCODED));
        mediaTypeMap.put(MEDIA_TYPE_MULTIPART, MediaType.parse(MEDIA_TYPE_MULTIPART));
    }

    private OkHttpClient client;


    public NeoHttpClient() {
        client = new OkHttpClient.Builder().connectTimeout(CONN_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS).readTimeout(READ_TIMEOUT, TimeUnit.SECONDS).build();
    }

    public Map<String, String> get(String url, Map<String, String> headerMap) throws Exception {
        Headers headers = buildHeaders(headerMap, url);
        Map<String, String> resultMap = new HashMap<>();
        Request request = new Request.Builder().url(url).headers(headers).build();
        try (Response response = client.newCall(request).execute()) {
            Headers rspHeaders = response.networkResponse().headers();
            int responseHeadersLength = rspHeaders.size();
            for (int i = 0; i < responseHeadersLength; i++){
                String headerName = rspHeaders.name(i);
                String headerValue = rspHeaders.get(headerName);
                System.out.print("TAG----------->Name:"+headerName+"------------>Value:"+headerValue+"\n");
            }



            Map<String, List<String>> rspHeaderMap = rspHeaders.toMultimap();
            rspHeaderMap.forEach((k, v) -> {
                resultMap.put(k, v.get(0));
            });

            if ("gzip".equalsIgnoreCase(response.header("Content-Encoding"))) {
                GzipSource source = new GzipSource(response.body().source());
                String charset = "UTF-8";
                String contentType = response.header("Content-Type");
                charset = split(contentType);
                String body = Okio.buffer(source).readString(Charset.forName(charset));
                resultMap.put("body", body);
            } else {
                String body = response.body().string();
                resultMap.put("body", body);
            }
            String cnapi=HttpUtil.httpGet(false,url,headerMap);

            return resultMap;
        }
    }

    private String split(String contentType) {
        String[] s = contentType.split(";");
        String ss = s[1];
        return StringUtils.replace(ss, "charset=", "");

    }


    public Map<String, String> post(String url, String content, String mediaType, Map<String, String> headerMap)
            throws Exception {
        Headers headers = buildHeaders(headerMap, url);
        Map<String, String> resultMap = new HashMap<>();

        RequestBody reqBody = RequestBody.create(mediaTypeMap.get(mediaType), content);
        Request request = new Request.Builder().url(url).headers(headers).post(reqBody).build();
        try (Response response = client.newCall(request).execute()) {

            Headers rspHeaders = response.headers();


            Map<String, List<String>> rspHeaderMap = rspHeaders.toMultimap();
            rspHeaderMap.forEach((k, v) -> {
                resultMap.put(k, v.get(0));
            });

            if ("gzip".equalsIgnoreCase(response.header("Content-Encoding"))) {
                GzipSource source = new GzipSource(response.body().source());
                String charset = "UTF-8";
                String contentType = response.header("Content-Type", "text/html;charset=UTF-8");
                charset = contentType.split(";")[1].replace("charset=", "");
                String body = Okio.buffer(source).readString(Charset.forName(charset));
                resultMap.put("body", body);
            } else {
                String body = response.body().string();
                resultMap.put("body", body);
            }
            return resultMap;
        }
    }

    private Headers buildHeaders(Map<String, String> headerMap, String url) {
        return Headers.of(headerMap);
    }


    public static void main(String args[]) {
        NeoHttpClient neo = new NeoHttpClient();
        Map<String, String> hp = new HashMap<>(8);
        hp.put("Content-Type", "text/xml; charset=utf-8");
        hp.put("Accept", "application/soap+xml,application/dime,multipart/related, text/*");
       // hp.put("Accept-Encoding", "gzip");

        try {
            Map<String, String> respmap = neo.get("http://wwww.baidu.com", hp);

            System.out.println(respmap.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
