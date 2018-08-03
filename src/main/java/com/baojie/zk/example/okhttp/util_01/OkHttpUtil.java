package com.baojie.zk.example.okhttp.util_01;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OkHttpUtil {

    private static final Logger log = LoggerFactory.getLogger(OkHttpUtil.class);
    private static final AtomicInteger seq = new AtomicInteger(0);
    private static final ConcurrentHashMap<Integer, OkHttpClient> closeMap = new ConcurrentHashMap<>();
    private static final ClientHook hook = new ClientHook();
    private static volatile boolean addHook = false;
    private static final TimeUnit UNIT = TimeUnit.SECONDS;
    private static int CONN_TIMEOUT = 30;
    private static int READ_TIMEOUT = 60;
    private static int WRITE_TIMEOUT = 60;

    private OkHttpUtil() {
        throw new IllegalArgumentException();
    }

    public static OkHttpClient client() {
        OkHttpUtil.addHook();
        final OkHttpClient client = create(pool());
        final int s = seq.getAndIncrement();
        try {
            return client;
        } finally {
            if (null != client) {
                closeMap.putIfAbsent(s, client);
            }
        }
    }

    private static ConnectionPool pool() {
        return new ConnectionPool(20, 15, TimeUnit.MINUTES);
    }

    private static OkHttpClient create(final ConnectionPool pool) {
        return new OkHttpClient.Builder()
                .connectionPool(pool)
                .writeTimeout(WRITE_TIMEOUT, UNIT)
                .readTimeout(READ_TIMEOUT, UNIT)
                .connectTimeout(CONN_TIMEOUT, UNIT)
                .build();
    }

    private static void addHook() {
        boolean a = addHook;
        if (a) {
            return;
        } else {
            synchronized (OkHttpUtil.class) {
                a = addHook;
                if (!a) {
                    Runtime.getRuntime().addShutdownHook(hook);
                    addHook = true;
                }
            }
        }
    }

    private static final class ClientHook extends Thread {

        public ClientHook() {

        }

        @Override
        public void run() {
            try {
                for (OkHttpClient c : closeMap.values()) {
                    OkClose.close(c);
                }
            } finally {
                closeMap.clear();
            }
        }

    }

}
