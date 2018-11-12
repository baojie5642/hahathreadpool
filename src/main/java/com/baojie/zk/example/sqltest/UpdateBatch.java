package com.baojie.zk.example.sqltest;

import java.util.concurrent.atomic.AtomicLong;

public class UpdateBatch {

    public static void main(String args[]) {
        final HikariDS ds = HikariDS.create("root", "123456");
        final AtomicLong state = new AtomicLong(0);
        CurrentAccess access = null;
        for (int i = 0; i < 1; i++) {
            access = new CurrentAccess(ds, 176, state);
            Thread t = new Thread(access, "thread_" + i);
            t.start();
        }
    }

}
