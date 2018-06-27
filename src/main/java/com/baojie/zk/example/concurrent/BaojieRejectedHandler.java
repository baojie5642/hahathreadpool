package com.baojie.zk.example.concurrent;

import com.baojie.zk.example.concurrent.seda.Stage;
import com.baojie.zk.example.concurrent.threadpool.MyThreadPool;

import java.util.concurrent.ThreadPoolExecutor;

// 接口的adaptor（转换器）
public abstract class BaojieRejectedHandler implements PoolRejectedHandler {
    private static final String DEFAULT_NAME = "global_reject_handler";
    private final String name;

    public BaojieRejectedHandler(String name) {
        this.name = (null == name ? DEFAULT_NAME : name);
    }

    @Override
    public void rejectedExecution(Runnable r, BaojieThreadPool executor) {

    }

    @Override
    public void rejectedExecution(Runnable r, ConcurrentPool executor) {

    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPool executor) {

    }

    @Override
    public void rejectedExecution(Runnable r, HaThreadPool executor) {

    }

    @Override
    public void rejectedExecution(Runnable r, MyThreadPool pool) {

    }

    @Override
    public void rejectedExecution(Runnable r, Stage pool) {

    }

    public String getName() {
        return name;
    }
}
