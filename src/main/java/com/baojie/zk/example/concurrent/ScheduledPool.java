package com.baojie.zk.example.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

public class ScheduledPool extends ScheduledThreadPoolExecutor {

    public ScheduledPool(int corePoolSize) {
        super(corePoolSize);
    }

    public ScheduledPool(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public ScheduledPool(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public ScheduledPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    public void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
    }

    // 防止使用submit方法的时候，当调用run发生异常时，异常被包装在future中
    @Override
    public void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        AfterExecute.afterExecute(runnable, throwable);
    }

}