package com.baojie.zk.example.concurrent;

import com.baojie.zk.example.concurrent.seda.Stage;
import com.baojie.zk.example.concurrent.threadpool.MyThreadPool;

import java.util.concurrent.RejectedExecutionHandler;

public interface PoolRejectedHandler extends RejectedExecutionHandler {

    void rejectedExecution(Runnable r, BaojieThreadPool executor);

    void rejectedExecution(Runnable r, ThreadPool executor);

    void rejectedExecution(Runnable r, HaThreadPool executor);

    void rejectedExecution(Runnable r, ConcurrentPool executor);

    void rejectedExecution(Runnable r, MyThreadPool pool);

    void rejectedExecution(Runnable r, Stage pool);

}
