package com.baojie.zk.example.concurrent.seda_refactor_01;

import com.baojie.zk.example.concurrent.TFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Stage<B extends Bus> extends AbstractStageService {

    private static final Logger log = LoggerFactory.getLogger(Stage.class);
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    private static final int STOP = 1 << COUNT_BITS;
    private static final int TIDYING = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    private final static long DEFAULT_ALIVE=180;
    private final TimeUnit unit = TimeUnit.SECONDS;
    private final BlockingQueue<StageTask> workQueue;
    private final HashSet<Worker> workers = new HashSet<>();
    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    private final ThreadFactory threadFactory;
    private final StageRejected handler;
    private final boolean elas;
    private final String name;
    private final Bus bus;

    private int largestPoolSize;
    private long completedTaskCount;
    private volatile int corePoolSize;
    private volatile long keepAliveTime;
    private volatile int maximumPoolSize;
    private final AccessControlContext acc;
    private volatile boolean allowCoreThreadTimeOut;
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    // 默认使用弹性队列,无任务缓存
    public Stage(int core, int max, long keepAlive, String name, Bus bus) {
        this(core, max, keepAlive, true, name, bus);
    }

    public Stage(int core, int max, long keepAlive, boolean elas, String name, Bus bus) {
        if (core < 0 || max <= 0 || max < core || keepAlive < 0) {
            throw new IllegalArgumentException();
        }
        if (unit == null || name == null || bus == null) {
            throw new NullPointerException();
        }
        this.bus = bus;
        // 添加强制校验
        if (!(bus instanceof Bus)) {
            throw new IllegalStateException();
        }
        this.elas = elas;
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = core;
        this.maximumPoolSize = max;
        // 暂时不使用缓存队列
        if (elas) {
            this.workQueue = new SynchronousQueue<>();
        } else {
            this.workQueue = new LinkedBlockingQueue<>();
        }
        this.keepAliveTime = unit.toNanos(keepAlive);
        this.name = name;
        this.threadFactory = TFactory.create(name);
        // 暂时实现直接失败的拒绝策略，简单
        this.handler = new DirectFail();
    }

    public B getBus() {
        return (B) bus;
    }

    // 这里的worker要实现runnable，方便start回调
    // 而内部运行的确实外面的runWorker
    // 这样通过试内部的原有的runnable对象的firstTask变换成自定义的接口
    // 从而完成相应的stage的运行
    // 但是要改造外名的future的内容才行，这样的改造才能使future正常工作
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;
        final Thread thread;
        StageTask firstTask;
        volatile long completedTasks;

        Worker(StageTask firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        @Override
        public void run() {
            runWorker(this);
        }

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            } else {
                return false;
            }
        }

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
            } catch (Throwable te) {
                log.error(te.toString() + ", callFrom=" + callFrom, te);
            }
        }
    }

    private ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState)) {
                break;
            } else if (ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    private final void tryTerminate() {
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
            final ReentrantLock lock = this.mainLock;
            lock.lock();
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
                lock.unlock();
            }
        }
    }

    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock lock = this.mainLock;
            lock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                lock.unlock();
            }
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        w.workerInterrupt(t, "Stage.interruptIdleWorkers");
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    // 与线程池的代码一致，支持shutDown方法，此方法主要用于停止那些已经启动的线程
    private void interruptWorkers() {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            lock.unlock();
        }
    }

    private static final boolean ONLY_ONE = true;

    @Override
    public boolean execute(StageTask task) {
        if (task == null) {
            return false;
        }
        int c = ctl.get();
        // 因为使用弹性队列所以一开始就会创建线程去执行
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(task, true)) {
                return true;
            } else {
                // 如果不成功，那么再次判断状态
                c = ctl.get();
            }
        }
        // 因为使用的sync阻塞队列
        // 所以如果放入成功
        // 那么说明已经已经被其他的线程获取到了
        if (isRunning(c) && workQueue.offer(task)) {
            int recheck = ctl.get();
            // 这里不能随便remove
            if (!isRunning(recheck)) {
                // 不是弹性的工作队列才能够移除
                if (!elas && remove(task)) {
                    reject(task);
                    return false;
                } else {
                    // 因为移除失败或者是弹性队列
                    // 所以这里返回成功，因为任务已被消费
                    tryTerminate();
                    return true;
                }
            } else if (workerCountOf(recheck) == 0) {
                addWorker(null, false);
                return true;
            } else {
                // 具体这里的所属类别还要参考业务
                return true;
            }
        } else if (!addWorker(task, false)) {
            reject(task);
            return false;
        } else {
            // 具体这里的所属类别还要参考业务
            return true;
        }
    }

    private boolean remove(StageTask task) {
        boolean removed = workQueue.remove(task);
        tryTerminate();
        return removed;
    }

    // 这里刚开始不去做task的判null
    // 因为允许先初始化线程线程等待任务执行
    private final boolean addWorker(StageTask firstTask, boolean core) {
        retry:
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {
                return false;
            }
            for (; ; ) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY) {
                    return false;
                } else if (wc >= (core ? corePoolSize : maximumPoolSize)) {
                    return false;
                }
                if (compareAndIncrementWorkerCount(c)) {
                    break retry;
                }
                c = ctl.get();
                if (runStateOf(c) != rs) {
                    continue retry;
                }
            }
        }
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock lock = this.mainLock;
                lock.lock();
                try {
                    int rs = runStateOf(ctl.get());
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) {
                            // 从业务上的层面来看，是否要直接抛出异常？还是返回false？
                            // 暂时定为抛出异常，因为这种错误很难出现
                            log.error("new Worker Thread isAlive, stage task=" + firstTask);
                            throw new IllegalThreadStateException();
                        }
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    lock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted) {
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }

    private void addWorkerFailed(Worker w) {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            if (w != null) {
                workers.remove(w);
            }
            decrementWorkerCount();
            tryTerminate();
        } finally {
            lock.unlock();
        }
    }

    private final void runWorker(Worker w) {
        // 这里的thread就是worker中的thread
        Thread wt = Thread.currentThread();
        StageTask task = w.firstTask;
        w.firstTask = null;
        w.unlock();
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() &&
                        runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.task(bus);
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

    private final StageTask getTask() {
        boolean timedOut = false;
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }
            int wc = workerCountOf(c);
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
            if ((wc > maximumPoolSize || ((timed && timedOut)) && (wc > 1 || workQueue.isEmpty()))) {
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                } else {
                    continue;
                }
            }
            try {
                StageTask r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) {
                    return r;
                } else {
                    timedOut = true;
                }
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    private final void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            lock.unlock();
        }
        tryTerminate();
        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                // 如果使用弹性队列，并且允许核心超时，那么如果线程数为0，还是可能直接返回的
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min) {
                    return;
                }
            }
            // 因为如果使用弹性队列，那么其实是在submit的时候就会先创建线程
            // 也就是说线程数是先增长的，所以这里可以执行再添加的方法
            addWorker(null, false);
        }
    }

    private final void reject(StageTask command) {
        handler.reject(command, this, name);
    }

    // 目前只允许直接调用shutDown，去掉shutDownNow方法
    // 并且使用无缓存的任务队列
    @Override
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    @Override
    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    @Override
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            for (; ; ) {
                if (runStateAtLeast(ctl.get(), TERMINATED)) {
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
    }

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

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
        } else if (delta > 0) {
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty()) {
                    break;
                }
            }
        }
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
                addWorker(null, true);
    }

    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value) {
                interruptIdleWorkers();
            }
        }
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) {
            interruptIdleWorkers();
        }
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        if (time == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0) {
            interruptIdleWorkers();
        }
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    // 暂时仅仅去停止无用的多余线程
    public void purge() {
        tryTerminate();
    }

    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                    : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked()) {
                    ++n;
                }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked()) {
                    ++nactive;
                }
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                        "Shutting down"));
        return super.toString() +
                "[" + rs +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    protected void beforeExecute(Thread t, StageTask r) {

    }

    protected void afterExecute(StageTask r, Throwable t) {
        if (null == t) {
            return;
        } else {
            log.error("occur error=" + t.toString() + ", stage task=" + r, t);
            // 是否应该在bus中添加相应的执行出错方法？待定
        }
    }

    protected void terminated() {

    }

    protected void onShutdown() {
    }

    public static class DirectFail implements StageRejected {

        public DirectFail() {

        }

        @Override
        public void reject(StageTask r, StageExecutor e, String reason) {
            log.error("rejected stage task=" + r + ", stage name=" + reason);
        }

    }

}