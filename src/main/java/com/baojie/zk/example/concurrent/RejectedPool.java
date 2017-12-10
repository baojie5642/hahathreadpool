package com.baojie.zk.example.concurrent;

import java.util.concurrent.*;

public class RejectedPool extends ThreadPoolExecutor {
    private static volatile RejectedPool Instance;

    // 这里的拒绝处理策略是循环的，一直会不断的提交RejectedHandler.getInstance()
    // 如果任务量过大，并且全部是拒绝发生，会出现这两个类的方法会不断的循环调用，直到runnable被执行
    public static RejectedPool getInstance() {
        if (null != Instance) {
            return Instance;
        } else {
            synchronized (RejectedPool.class) {
                if (null == Instance) {
                    Instance = new RejectedPool(8, 512, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(2048),
                            UnitedThreadFactory.create("Global_RejectedPool"),
                            RejectedHandler.getInstance());
                }
            }
            return Instance;
        }
    }

    public RejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public RejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public RejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public RejectedPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
    }

    // 防止使用submit方法的时候，当调用run发生异常时，异常被包装在future中
    @Override
    public void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        AfterExecute.afterExecute(runnable, throwable);
    }

    public static void shutGlobalRejectedPool() {
        PoolShutDown.executor(Instance, "Global_RejectedPool");
    }

}