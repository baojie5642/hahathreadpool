package com.baojie.zk.example.concurrent;

import java.util.concurrent.RejectedExecutionHandler;

public interface PoolRejectedHandler extends RejectedExecutionHandler {

    void rejectedExecution(Runnable r, BaojieThreadPool executor);

    void rejectedExecution(Runnable r, ThreadPool executor);

}
