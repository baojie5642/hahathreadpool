package com.baojie.zk.example.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UnitedThreadFactory implements ThreadFactory {
    private static final UnitedUncaught UNCAUGHT_EXCEPTION_HANDLER = UnitedUncaught.getInstance();
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private static final int NO_THREAD_PRIORITY = Thread.NORM_PRIORITY;
    private final AtomicLong t_number = new AtomicLong(1);
    private final ThreadGroup group;
    private final String factoryName;
    private final int t_Priority;
    private final String poolName;
    private final boolean isDaemon;

    public static UnitedThreadFactory create(final String name) {
        return new UnitedThreadFactory(name, false, NO_THREAD_PRIORITY);
    }

    public static UnitedThreadFactory create(final String name, final boolean isDaemon) {
        return new UnitedThreadFactory(name, isDaemon, NO_THREAD_PRIORITY);
    }

    public static UnitedThreadFactory create(final String name, final int threadPriority) {
        return new UnitedThreadFactory(name, false, threadPriority);
    }

    public static UnitedThreadFactory create(final String name, final boolean isDaemon, final int threadPriority) {
        return new UnitedThreadFactory(name, isDaemon, threadPriority);
    }

    private UnitedThreadFactory(final String name, final boolean isDaemon, final int threadPriority) {
        this.group = UnitedThreadGroup.getGroup();
        this.factoryName = name;
        this.isDaemon = isDaemon;
        this.t_Priority = threadPriority;
        this.poolName = factoryName + "_" + POOL_NUMBER.getAndIncrement();
    }

    @Override
    public Thread newThread(final Runnable r) {
        final String threadNum = String.valueOf(t_number.getAndIncrement());
        final Thread thread = new Thread(group, r, poolName + "_thread_" + threadNum, 0);
        setProperties(thread);
        return thread;
    }

    private void setProperties(final Thread thread) {
        setDaemon(thread);
        setPriority(thread);
        thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }

    private void setDaemon(final Thread thread) {
        if (isDaemon == true) {
            thread.setDaemon(true);
        } else {
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
        }
    }

    private void setPriority(final Thread thread) {
        if (t_Priority == NO_THREAD_PRIORITY) {
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
        } else {
            final int priority = checkPriority();
            thread.setPriority(priority);
        }
    }

    private int checkPriority() {
        if (t_Priority <= Thread.MIN_PRIORITY) {
            return Thread.MIN_PRIORITY;
        } else {
            if (t_Priority >= Thread.MAX_PRIORITY) {
                return Thread.MAX_PRIORITY;
            } else {
                return t_Priority;
            }
        }
    }

    public static UnitedUncaught getUncaught() {
        return UNCAUGHT_EXCEPTION_HANDLER;
    }

    public int getPoolNumber() {
        return POOL_NUMBER.get();
    }

    public long getThreadNumber() {
        return t_number.get();
    }

    public String getFactoryName() {
        return factoryName;
    }

    public int getThreadPriority() {
        return t_Priority;
    }

    public String getPoolName() {
        return poolName;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

}