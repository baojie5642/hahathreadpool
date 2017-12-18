package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HaThreadPool  extends AbstractExecutorService {

    private static final Logger log = LoggerFactory.getLogger(HaThreadPool.class);
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    private static final int STOP = 1 << COUNT_BITS;
    private static final int TIDYING = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    // Packing and unpacking ctl
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     */
    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    private final BlockingQueue<Runnable> workQueue;

    private final ReentrantLock mainLock = new ReentrantLock();

    private final HashSet<Worker> workers = new HashSet<Worker>();

    private final Condition termination = mainLock.newCondition();

    private int largestPoolSize;

    private long completedTaskCount;

    private volatile ThreadFactory threadFactory;

    private volatile PoolRejectedHandler handler;

    private volatile long keepAliveTime;

    private volatile boolean allowCoreThreadTimeOut;

    private volatile int corePoolSize;

    private volatile int maximumPoolSize;

    private static final PoolRejectedHandler defaultHandler = new BaojieThreadPool.AbortPolicy();

    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    /* The context to be used when executing the finalizer, or null. */
    private final AccessControlContext acc;


    // 继承AbstractQueuedSynchronizer原因之一：1.控制当前线程的运行或者idle状态
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;
        final Thread thread;
        Runnable firstTask;
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker
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
            }
            return false;
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
            // 1.如果已经被获取锁 2.容器线程不等于null 3.并且当前的线程还没有被中断
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

    /*
     * Methods for setting control state
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *                    (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
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

    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     */
    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            // 如果状态为运行，返回
            if (isRunning(c)) {
                return;
            } else if (runStateAtLeast(c, TIDYING)) {
                // 如果状态已经是正在被终止，返回
                return;
            } else if ((runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                // 如果状态为shutDown，并且queue里面还有任务，那么再设计上是会自动取空为止的
                // 取完之后会自动终止所有线程，所以返回
                return;
            }
            // 如果为其他状态，说明，在快要完全停止pool之前，还提交了以任务进来，又因为此时队列为空
            // 说明空闲的就只这个线程，虽然可能没有启动，但是还是要停止
            // 还有就是如果运行的线程都在queue poll wait上，那么这个就有连级唤醒作用
            if (workerCountOf(c) != 0) { // Eligible to terminate
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
            // else retry on failed CAS
        }
    }

    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        w.workerInterrupt(t, "ThreadPool.interruptIdleWorkers");
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    void onShutdown() {
    }

    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        // 有些队列，比如延迟队列，这个操作会失败，所以要一个一个的转移出来
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (; ; ) {
            // 获取当前线程池的状态
            int c = ctl.get();
            int rs = runStateOf(c);
            // Check if queue empty only if necessary.
            // 情况如下：
            // 1.如果线程池还没shutDown,继续向下执行
            // 2.如果线程池已经shutDown,并且此时runner==null且队列不为空,继续向下执行
            // 3.如果线程池已经shutDown,并且此时runner==null且队列为空,提交失败return false
            // 4.如果线程池已经shutDown,并且此时runner!=null且队列不为空,提交失败return false
            // 5.如果线程池已经shutDown,并且此时runner!=null且队列为空,提交失败return false
            // 6.如果线程池已经shutDown,并且已经终止,提交失败return false
            // 7.runner为null的情况需要再判断了
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {
                return false;
            }
            for (; ; ) {
                int wc = workerCountOf(c);
                // 超出'integer(近似)'最大数量,提交失败
                if (wc >= CAPACITY) {
                    return false;
                } else if (wc >= (core ? corePoolSize : maximumPoolSize)) {
                    // 超出设置的最大数量(core或者max),提交失败
                    return false;
                }
                // 如果增长线程池数量时失败,跳出循环
                // 这个和下面的再次判断线程池的状态是相关联的
                if (compareAndIncrementWorkerCount(c)) {
                    break retry;
                }
                c = ctl.get();  // Re-read ctl
                // 再次获取当前线程池状态,如果变化了,再来一次
                if (runStateOf(c) != rs) {
                    continue retry;
                }
                // else CAS failed due to workerCount change; retry inner loop
            }
        }
        // 如果进行到这里,说明所有的状态已经全部同步,将要进行真正的run了,add完成就run
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            // 这个worker自带同步状态控制,并且让自己run在自己的成员变量thread里面,
            // 可以通过调用自身的方法去中断为自己提供run空间的thread容器(可以将这里的内部thread看成runnable的容器)
            w = new Worker(firstTask);
            final Thread t = w.thread;
            // 如果threadFactory创建线程失败了,那么失败
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 再次检测状态,消除由于竞争导致的状态不一致或者比如线程工厂的一些错误发生导致状态变化
                    int rs = runStateOf(ctl.get());
                    // 如果小于shutdown说明离停止还很早，继续执行，不在判断了
                    // 如果状态已经shutdown，还要继续判断，如果正好shutDown，并且Runner为null继续执行
                    // 如果状态大于shutdown，已经终止了或者正在终止，那么提交失败
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }
                        workers.add(w);
                        int s = workers.size();
                        // 保存线程池在运行期间所能达到的最大的运行线程的数量，只增不减
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                // 状态设置的连级，呼应下面的workerStarted，与流水线模型类比
                if (workerAdded) {
                    // 这个worker的数据结构很巧妙，start的是worker的run，run运行的是threadpool的runWorker，很巧
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
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) {
                workers.remove(w);
            }
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // If abrupt, then workerCount wasn't adjusted
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 不管什么情况，都是完成了
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        int c = ctl.get();
        // 如果状态离停止还差的很远，那么说明可能运行的线程正好到时间了（queue poll wait）
        // 防止此时此刻正好有任务提交但是没有worker去执行的情况
        // 如果已经快要完全的停止了，那么交由连级唤醒的线程来处理，直接返回
        if (runStateLessThan(c, STOP)) {
            // 如果线程异常停止，保持线程池的稳定，不影响其他任务的处理
            // 如果正常结束，completedAbruptly=false，保证queue中任务正常消费
            // 如果异常结束，那么状态的判断交给addWorker去判断
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                // 核心线程正好timeout
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                // 如果连一个工作的runner都没有，是不能直接返回的
                // 正常结束的情况下，如果还有很多worker那么直接返回，交给其他的代码执行到这里的worker去完成
                if (workerCountOf(c) >= min) {
                    return; // replacement not needed
                }
            }
            // 添加一个null进入任务，防止任务队列不能正常消费掉
            addWorker(null, false);
        }
    }

    private Runnable getTask() {
        // Did the last poll() time out?
        // 如果是正常timeout=true，说明是正常的情况，否则说明被中断了，timeout=false，结合下面的变化来看
        boolean timedOut = false;
        for (; ; ) {
            int c = ctl.get();
            // 获取当前线程池的状态
            int rs = runStateOf(c);
            // Check if queue empty only if necessary.
            // 判断是已经停止了还是正在完成停止（但是还没有完全的停止，任务队列中还有任务）
            // 如果已经停止了，那么改变工作runner数量是必须的，保持状态的一致（状态的同步）
            // 如果当前线程正在执行从shutdown到stop的转变，那么这时需要判断任务队列中是否还有任务没有执行完
            // 如果没任务了，那么同步数量状态，如果有，继续下面的语句，不返回null
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }
            // 计算工作者的数量
            int wc = workerCountOf(c);
            // Are workers subject to culling?
            // 判断是否开启了允许核心线程数timeout或者此时的工作者线程数已经超过了设置的核心数量
            // 如果开启了允许核心timeout那么采用poll方式获取任务
            // 或者
            // 如果当前数量已经大于核心数量，同样采取poll方式，是否开启允许核心超时是优先级更高的判断
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
            // 好了，下面的判断是重点了，经典设计，注意了
            // 1.为什么比较maximumPoolSize？如果此时显示的调用了设置setMaximumPoolSize，那么这时是要判断的
            // 原子操作数量，只会有一个是成功的，不做多余的减法操作，确保工作者数量是可靠的
            // 因为compareAndDecrementWorkerCount一旦成功了那么其他的线程调用此方法会失败，那么就会继续去getTask
            // 2.timed是为了判断是否要清理掉多余的工作者数量，这里所谓的多余是同核心线程数比较的，如果开启了允许核心线程数timeout
            // 那么可以理解为核心线程数=0，也就是说是否开启允许核心超时是优先级更高的判断
            // 3.timedOut，如果是正常timeout=true，说明是正常的情况，否则说明被中断了，timeout=false，结合下面的变化来看
            // 4.为什么wc > 1要与workQueue.isEmpty()一起判断呢？
            // 如果是一个工作者，那么确保在shutDown或者pool的状态发生变化时这一个worker都能顺利根据状态取完任务队列中的任务
            // 如果一个worker，正好运行完任务，此时pool也没有调用关闭的相关方法，那么还要确保此时也会去任务队列中取任务执行
            // 防止没终止但是没人执行任务的情况出现，上面的方法里面也有防止此种情况出现的手段，参上
            // 如果有很多worker，那就好办了，不用判断了，继续就好了，wc > 1 || workQueue.isEmpty()注意判断的优先级
            // 5.说一下且'&&'的关系，为什么要用且的关系呢？下面会有很多的如果，仔细看
            // 如果应该结束掉多余的worker，并且此时执行此条语句的线程是正常的timeout，并且（还有很多worker||taskQueue已经空了）
            // 那么这时才会真正的return null（当然，是在compareAndDecrementWorkerCount成功后）
            // 如果wc > maximumPoolSize，很自然的是要结束多余工作者的（maximumPoolSize不能 <= 0，看下面的set方法）
            // 如果不需要清理多余的工作者(timed=false && timedOut)=false，这时只需要判断maximumPoolSize就好了，
            // 因为后面的已经为false了
            // 如果第一次进入此for循环或者调用了shutDown或者显示中断，那么同样判断maximumPoolSize就好了，这时的资源处理工作交给
            // processWorkerExit，在runWorker方法里面的finally方法里面可见
            // 如果wc < maximumPoolSize，那么要看是否要清理掉多余工作者，还要看这时的工作者数量
            // 注意 ((wc > maximumPoolSize || ((timed && timedOut)) && (wc > 1 || workQueue.isEmpty())))里面的
            // 判断优先级还有且'&&'的关系，'||'或关系是带有优先级的
            // 6.这个要从整体上并结合pool的状态来领会这些判断就好了，然后就很清晰了，嘎嘎
            // 7.并发的设计不外乎就是数据状态的同步控制，哈哈
            // 8.顺便说下，同步，并发，异步，并行这些前三个是同源的，同源也就是都是要求有规则的处理任务，但是不同形，也就是
            // 表现出来形式不同而已，都是一样的，而最后一个并行就要看任务的相关性了，如果不相关其实就可以把每个都看成独立的，然后
            // 递归的理解一下就好了，如果相关，是不是问题就又回去了呢？哈哈，是的，又绕回去了
            if ((wc > maximumPoolSize || ((timed && timedOut)) && (wc > 1 || workQueue.isEmpty()))) {
                // 确保数量状态同步一致
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
                // 如果已经有其他线程成功了那么当前的工作者线程继续for循环，看下我自己是否应该get到Task
                // 也就是继续去判断状态，执行然后根据状态执行正确的流程
                // 注意，下面的InterruptedException异常，针对thread而言就是个状态，那么状态是要去判断才知道的，所以
                // 可以写一个for循环去测试一下，在代码的那个位置上采取判断中断状态，也很简单，thread可以设置中断状态，也可以
                // 擦除中断状态，有些代码里面就是这么做的，嘻嘻
                continue;
            }
            try {
                // 如果应该清理掉多余工作者线程，那么采取poll的方式，否则使用take的触发式的模式获取task
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) {
                    return r;
                }
                // 如果是正常的poll超时或者take被唤醒，那么设置此状态，区别中断的情况
                // 这里的中断是由于外部线程调用线程池的方法，改变了状态，才导致的内部的worker的数据结构中的thread成员变量的中断
                // 比如调用shutDown或者purge
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }


    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        // 同步状态控制
        w.unlock(); // allow interrupts
        // 判断流程中是否出错的标志
        boolean completedAbruptly = true;
        try {
            // 通过getTask同步的获取task
            // 可以看出，runner是晕允许为null的也就是可以初始化一个worker等待任务到来
            // 同时也是为了替换出错线程来设计的，执行的线程出错了，并且下面的thrown不为null的时候，会开启
            // 一个runner为null的worker来替换原有出错的worker
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                // 1.如果pool的状态已经是stop或者远大于stop那么中断执行的worker
                // 2.如果线程池的状态距离stop还很远，那么再判断当前的线程是否被中断，如果没有被中断，进入try语句
                // 3.如果被中断，但是pool的状态距离stop还很远，那么同样进入try语句
                // 4.如果被中断，并且pool的状态已经是stop或者远大于stop了，那么执行wt.isInterrupted()
                // 由于Thread.interrupted()已经擦出了中断状态，所以加了！
                // 但是如果线程被中断2此或者在执行Thread.interrupted()之后被中断多次，那么还是进入try语句块，但是中断多次是不会发生的
                // 因为是pool的内部自己中断的自己，做了同步控制，所以这种情况不会发生
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(),
                        STOP))) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    // 执行子类的方法
                    beforeExecute(wt, task);
                    // 保存执行过程中的异常
                    Throwable thrown = null;
                    try {
                        // run的是谁？又是谁调了run？
                        // 提示：1.worker数据结构的设计  2.submit提交与execute执行的区别
                        // 如果submit提交这个task是一个futureTask，请参阅futureTask代码
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
                        // 如果thrown！=null，并且在afterExecute中同样抛出异常，那么此时会冲掉thrown
                        // 如果采用submit方式提交任务，这里出现的异常会封装到future中，在afterExecute中传进的thrown为null
                        afterExecute(task, thrown);
                    }
                } finally {
                    // 资源清理，显示的置为null
                    task = null;
                    // 计数完成的任务量，出错也是完成了，同样加入计数
                    w.completedTasks++;
                    // 同步控制worker的状态，下一篇再去深入的跑西一下那个同步器吧，喝口咖啡
                    w.unlock();
                }
            }
            // 如果出现异常并且在这里catch到了，这样，下面这行代码是不会执行的，并且finally是由内而外顺序被调用的
            // 如果是getTask()为null，那么是正常的跳出while循环，这时正常结束，这时completedAbruptly = false，那么至于
            // processWorkerExit(w, completedAbruptly)如何执行，请参阅这个方法吧，里面已经说明了
            completedAbruptly = false;
        } finally {

            processWorkerExit(w, completedAbruptly);
        }
    }

    // Public constructors and methods

    public HaThreadPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }


    public HaThreadPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }


    public HaThreadPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            PoolRejectedHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }


    public HaThreadPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            PoolRejectedHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }


    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {
            reject(command);
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * <p>
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     * <p>
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     * <p>
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
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
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     */
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

    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }


    public void setRejectedExecutionHandler(PoolRejectedHandler handler) {

    }

    public PoolRejectedHandler getRejectedExecutionHandler() {
        return handler;
    }

    public void setCorePoolSize(int corePoolSize) {

    }

    public int getCorePoolSize() {
        return corePoolSize;
    }


    public boolean prestartCoreThread() {
        return true;
    }

    void ensurePrestart() {

    }

    public int prestartAllCoreThreads() {
        return 1;
    }

    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean value) {

    }

    public void setMaximumPoolSize(int maximumPoolSize) {

    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {

    }

    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    public void purge() {

    }

    public int getPoolSize() {
        return 1;
    }


    public int getActiveCount() {
        return 1;
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
        return 1L;
    }

    public long getCompletedTaskCount() {
        return 1L;
    }

    protected void beforeExecute(Thread t, Runnable r) {
    }


    protected void afterExecute(Runnable r, Throwable t) {
    }


    protected void terminated() {
    }

    public static class CallerRunsPolicy extends BaojieRejectedHandler {
        public CallerRunsPolicy() {
            super("CallerRunsPolicy");
        }

        public void rejectedExecution(Runnable r, HaThreadPool e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    public static class AbortPolicy extends BaojieRejectedHandler {

        public AbortPolicy() {
            super("AbortPolicy");
        }

        public void rejectedExecution(Runnable r, HaThreadPool e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }

    }

    public static class DiscardPolicy extends BaojieRejectedHandler {
        public DiscardPolicy() {
            super("DiscardPolicy");
        }

        public void rejectedExecution(Runnable r, HaThreadPool e) {
        }
    }

    public static class DiscardOldestPolicy extends BaojieRejectedHandler {

        public DiscardOldestPolicy() {
            super("DiscardOldestPolicy");
        }

        public void rejectedExecution(Runnable r, HaThreadPool e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}


