package com.baojie.zk.example.concurrent.seda;

import com.baojie.zk.example.concurrent.TFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractStageService<T> implements StageService<T> {

    protected final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    protected static final int COUNT_BITS = Integer.SIZE - 3;
    protected static final int CAPACITY = (1 << COUNT_BITS) - 1;
    protected static final int RUNNING = -1 << COUNT_BITS;
    protected static final int SHUTDOWN = 0 << COUNT_BITS;
    protected static final int STOP = 1 << COUNT_BITS;
    protected static final int TIDYING = 2 << COUNT_BITS;
    protected static final int TERMINATED = 3 << COUNT_BITS;

    private static final boolean ONLY_ONE = true;

    protected static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    protected static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    protected static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    protected static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    protected static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    protected static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    protected static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    protected boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    protected boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    protected void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    protected void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState)) {
                break;
            } else if (ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    protected final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            if (isRunning(c)) {
                return;
            } else if (runStateAtLeast(c, TIDYING)) {
                return;
            } else if ((runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                return;
            }
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    protected abstract void terminated();

    protected abstract void interruptIdleWorkers(boolean onlyOne);

    protected final LinkedBlockingQueue<T> workQueue = new LinkedBlockingQueue<>(8192);
    protected final ReentrantLock mainLock = new ReentrantLock();
    protected final Condition termination = mainLock.newCondition();
    protected final ThreadFactory threadFactory;
    protected final String stageName;
    protected final AccessControlContext acc;
    protected volatile boolean allowCoreThreadTimeOut;
    protected volatile int corePoolSize;
    protected volatile long keepAliveTime;
    protected volatile int maximumPoolSize;

    public AbstractStageService(int core, int max, long keepAlive, TimeUnit unit, String name) {
        if (core < 0 || max <= 0 || max < core || keepAlive < 0) {
            throw new IllegalArgumentException();
        }
        if (unit == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = core;
        this.maximumPoolSize = max;
        this.keepAliveTime = unit.toNanos(keepAlive);
        if (null == name) {
            throw new NullPointerException();
        } else if (name.trim().length() == 0) {
            throw new IllegalArgumentException();
        } else {
            this.stageName = name;
            this.threadFactory = TFactory.create(stageName);
            this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        }
    }

    protected ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    @Override
    public abstract boolean submit(T task);

    @Override
    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> {
                shutdown();
                return null;
            };
            AccessController.doPrivileged(pa, acc);
        }
    }
}
