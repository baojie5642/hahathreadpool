package com.baojie.zk.example.okhttp.util_01;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OkClose {

    private static final Logger log = LoggerFactory.getLogger(OkClose.class);

    private OkClose() {
        throw new IllegalStateException();
    }

    public static void close(OkHttpClient c) {
        if (null != c) {
            closeDis(c);
            closeCon(c);
            closeCache(c);
        }
    }

    private static void closeDis(OkHttpClient c) {
        try {
            c.dispatcher().executorService().shutdown();
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
    }

    private static void closeCon(OkHttpClient c) {
        try {
            c.connectionPool().evictAll();
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
    }

    private static void closeCache(OkHttpClient c) {
        Cache cache = c.cache();
        if (null == cache) {
            return;
        }
        try {
            c.cache().close();
        } catch (IOException ie) {
            log.error(ie.toString(), ie);
        } catch (Throwable te) {
            log.error(te.toString(), te);
        }
    }

}
