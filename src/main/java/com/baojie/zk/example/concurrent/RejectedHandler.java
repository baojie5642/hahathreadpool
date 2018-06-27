package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class RejectedHandler implements RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(RejectedHandler.class);
    private static volatile RejectedHandler instance;
    private static final int LoopSubmit = 100;
    private static final int LoopTime = 10;
    private final String rejectedName;

    private RejectedHandler(String rejectedName) {
        this.rejectedName = rejectedName;
    }

    public static RejectedHandler create(final String rejectedName) {
        return new RejectedHandler(rejectedName);
    }

    public static RejectedHandler getInstance() {
        if (null != instance) {
            return instance;
        } else {
            synchronized (RejectedHandler.class) {
                if (null == instance) {
                    instance = RejectedHandler.create("Global_RejectedHandler");
                }
                return instance;
            }
        }
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (null == runnable || null == executor) {
            return;
        }
        Queue<Runnable> taskQueue = executor.getQueue();
        if (null == taskQueue) {
            return;
        }
        if (taskQueue.offer(runnable)) {
            return;
        } else {
            innerLoopSubmit(runnable, taskQueue, executor);
        }
    }

    private void innerLoopSubmit(Runnable runnable, Queue<Runnable> taskQueue, ThreadPoolExecutor executor) {
        int testLoop = 0;
        boolean loopSuccess = false;
        for (; testLoop <= LoopSubmit; ) {
            if (taskQueue.offer(runnable)) {
                loopSuccess = true;
                break;
            } else {
                testLoop++;
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(LoopTime, TimeUnit.MILLISECONDS));
            }
        }
        checkLoopState(loopSuccess, runnable, executor);
    }

    private void checkLoopState(boolean loopSuccess, Runnable runnable, ThreadPoolExecutor executor) {
        if (loopSuccess) {
            return;
        } else {
            submitRunnableIntoRejectedThreadPool(runnable);
            printThreadName(runnable, executor);
        }
    }

    private void submitRunnableIntoRejectedThreadPool(Runnable runnable) {
        RejectedPool.getInstance().submit(runnable);
    }

    private void printThreadName(Runnable r, ThreadPoolExecutor executor) {
        String tn = null;
        ThreadFactory tf = executor.getThreadFactory();
        if (null != tf && (tf instanceof TFactory)) {
            tn = ((TFactory) tf).getFactoryName();
        }
        log.warn("RejectedHandlerName=" + rejectedName + ", threadName=" + tn + ", runnable.toString=" + r.toString());
    }

    public String getRejectedHandlerName() {
        return rejectedName;
    }
}
