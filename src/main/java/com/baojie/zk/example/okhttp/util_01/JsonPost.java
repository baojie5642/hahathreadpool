package com.baojie.zk.example.okhttp.util_01;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.IOException;

public class JsonPost extends RequestBody {

    private final String json;

    public JsonPost(String json) {
        this.json = json;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/json; charset=utf-8");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.writeUtf8(json);
        sink.flush();
    }

}
