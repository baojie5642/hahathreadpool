package com.baojie.zk.example.okhttp.util_01;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.IOException;

public class BaseSoapPost extends RequestBody {

    private final String so;

    public BaseSoapPost(String so) {
        this.so = so;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("text/xml; charset=utf-8");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.writeUtf8(so);
        sink.flush();
    }

}
