package com.baojie.zk.example.concurrent.threadpool;

import com.baojie.zk.example.concurrent.BaojieRejectedHandler;
import com.baojie.zk.example.concurrent.PoolRejectedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class MyThreadPool extends AbstractPoolService {
    private static final Logger log = LoggerFactory.getLogger(MyThreadPool.class);
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    private static final int CAPACITY = Integer.MAX_VALUE - 3;
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int STOP = 2;
    private static final int TIDYING = 3;
    private static final int TERMINATED = 4;

    private final AtomicInteger parkKey = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<Runnable> workQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> workers = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, Worker> parks = new ConcurrentHashMap<>(8192);


    private final AccessControlContext acc;
    private volatile ThreadFactory threadFactory;
    private volatile PoolRejectedHandler handler;
    private volatile long keepAliveTime;
    private volatile int corePoolSize;
    private volatile int maxPoolSize;


    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;
        final Thread thread;
        volatile Runnable firstTask;
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        @Override
        public void run() {
            runWorker(this);
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        // shutDownNow调用的方法
        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                workerInterrupt(t, "Worker.interruptIfStarted");
            }
        }

        void workerInterrupt(Thread t, String callFrom) {
            if (null == t) {
                log.error("thread null, callFrom=" + callFrom);
                return;
            }
            try {
                t.interrupt();
            } catch (SecurityException ignore) {
                log.error(ignore.toString() + ", callFrom=" + callFrom, ignore);
            } catch (Throwable throwable) {
                log.error(throwable.toString() + ", callFrom=" + callFrom, throwable);
            }
        }

    }

    private void checkShutdownAccess(SecurityManager security, Thread thread) {
        if (security != null) {
            security.checkPermission(shutdownPerm);
            security.checkAccess(thread);
        }
    }

    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock();
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask(w)) != null) {
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    private Runnable getTask(Worker w) {
        return pollQueue();
    }

    private Runnable pollQueue() {
        return workQueue.poll();
    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) {

    }

    public MyThreadPool(int core, int max, long keep, TimeUnit unit) {
        this(core, max, keep, unit, Executors.defaultThreadFactory(), new MyThreadPool.AbortPolicy());
    }

    public MyThreadPool(int core, int max, long keep, TimeUnit unit, ThreadFactory tf) {
        this(core, max, keep, unit, tf, new MyThreadPool.AbortPolicy());
    }

    public MyThreadPool(int core, int max, long keep, TimeUnit unit, PoolRejectedHandler handler) {
        this(core, max, keep, unit, Executors.defaultThreadFactory(), handler);
    }

    public MyThreadPool(int core, int max, long keep, TimeUnit unit, ThreadFactory tf, PoolRejectedHandler handler) {
        if (core < 0 || max <= 0 || max < core || keep < 0 || max > CAPACITY) {
            throw new IllegalArgumentException();
        }
        if (tf == null || handler == null) {
            throw new NullPointerException();
        }
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = core;
        this.maxPoolSize = max;
        this.keepAliveTime = unit.toNanos(keep);
        this.threadFactory = tf;
        this.handler = handler;
    }

    public void execute(Runnable command) {

    }

    public void shutdown() {

    }

    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    public boolean isShutdown() {
        return true;
    }

    public boolean isTerminated() {
        return true;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }

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


    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public String toString() {
        return "";
    }

    protected void beforeExecute(Thread t, Runnable r) {
    }

    protected void afterExecute(Runnable r, Throwable t) {
    }


    public static class CallerRunsPolicy extends BaojieRejectedHandler {
        public CallerRunsPolicy() {
            super("CallerRunsPolicy");
        }

        public void rejectedExecution(Runnable r, MyThreadPool e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    public static class AbortPolicy extends BaojieRejectedHandler {

        public AbortPolicy() {
            super("AbortPolicy");
        }

        public void rejectedExecution(Runnable r, MyThreadPool e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }

    }

    public static class DiscardPolicy extends BaojieRejectedHandler {
        public DiscardPolicy() {
            super("DiscardPolicy");
        }

        public void rejectedExecution(Runnable r, MyThreadPool e) {
        }
    }

    public static class DiscardOldestPolicy extends BaojieRejectedHandler {

        public DiscardOldestPolicy() {
            super("DiscardOldestPolicy");
        }

        public void rejectedExecution(Runnable r, MyThreadPool e) {
            if (!e.isShutdown()) {

                e.execute(r);
            }
        }
    }

}
