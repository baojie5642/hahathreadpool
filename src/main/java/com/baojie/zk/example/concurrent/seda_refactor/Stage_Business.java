package com.baojie.zk.example.concurrent.seda_refactor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Stage_Business {

    public void bus() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(60));

        throw new Error("test error");
    }

}
