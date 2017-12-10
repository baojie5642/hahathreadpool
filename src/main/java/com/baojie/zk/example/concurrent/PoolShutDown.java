package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class PoolShutDown {
    private static final Logger log = LoggerFactory.getLogger(PoolShutDown.class);

    private PoolShutDown() {

    }

    public static void threadPool(final ThreadPool threadPool, final String shutDownReason) {
        if (null == threadPool) {
            log.error(shutDownReason + ", threadPool null");
            return;
        }
        if (threadPool.isShutdown()) {
            log.info(shutDownReason + ", threadPool shutDown complete");
            return;
        }
        int i = 0;
        for (; ; ) {
            threadPool.shutdown();
            if (threadPool.isShutdown()) {
                log.info(shutDownReason + ", threadPool shutDown complete");
                return;
            }
            ++i;
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
            if (i >= 60) {
                threadPool.shutdownNow();
                log.error(shutDownReason + ", threadPool call shutDownNow complete");
                return;
            }
        }
    }

    public static void executor(final ThreadPoolExecutor executor, final String shutDownReason) {
        if (null == executor) {
            log.error(shutDownReason + ", ThreadPoolExecutor null");
            return;
        }
        if (executor.isShutdown()) {
            log.info(shutDownReason + ", ThreadPoolExecutor shutDown complete");
            return;
        }
        int i = 0;
        for (; ; ) {
            executor.shutdown();
            if (executor.isShutdown()) {
                log.info(shutDownReason + ", ThreadPoolExecutor shutDown complete");
                return;
            }
            ++i;
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
            if (i >= 60) {
                executor.shutdownNow();
                log.error(shutDownReason + ", ThreadPoolExecutor call shutDownNow complete");
                return;
            }
        }
    }


    public static void scheduledPool(final ScheduledPool scheduledPool, final String shutDownReason) {
        if (null == scheduledPool) {
            log.error(shutDownReason + ", scheduledPool null");
            return;
        }
        if (scheduledPool.isShutdown()) {
            log.info(shutDownReason + ", scheduledPool shutDown complete");
            return;
        }
        int i = 0;
        for (; ; ) {
            scheduledPool.shutdown();
            if (scheduledPool.isShutdown()) {
                log.info(shutDownReason + ", scheduledPool shutDown complete");
                return;
            }
            ++i;
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS));
            if (i >= 60) {
                scheduledPool.shutdownNow();
                log.error(shutDownReason + ", scheduledPool call shutDownNow complete");
                return;
            }
        }
    }

}
