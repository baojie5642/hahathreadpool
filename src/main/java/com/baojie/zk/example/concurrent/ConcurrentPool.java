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

public class ConcurrentPool extends AbstractExecutorService {
    private static final Logger log = LoggerFactory.getLogger(BaojieThreadPool.class);
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

    private final HashSet<Worker> workers = new HashSet<>();

    private final Condition termination = mainLock.newCondition();

    private int largestPoolSize;

    private long completedTaskCount;

    private volatile ThreadFactory threadFactory;

    private volatile PoolRejectedHandler handler;

    private volatile long keepAliveTime;

    private volatile boolean allowCoreThreadTimeOut;

    private volatile int corePoolSize;

    private volatile int maximumPoolSize;

    private static final PoolRejectedHandler defaultHandler = new AbortPolicy();

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

    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     * <p>
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     * <p>
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     * <p>
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     * <p>
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     * <p>
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     * <p>
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    // 问题1.如果原来的pool中有100个core worker，其中78个执行时出现了异常，那么这时，
    // 再有任务提交进来时，此时pool有多少个worker？ 22个吗？ 仔细想想
    // 问题2.如果afterExecute抛出异常怎么办？
    // 问题3.这么多的finally，如果每个部分都抛出异常，执行的顺是怎样的？正常的顺序又是怎样的？
    // 问题4.如果采用submit方式提交，出现异常时，这时由谁创建下一个worker？要跳出while循环吗？
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
            // 如果使用的是submit的future形式提交的，那么出现异常时，捕获throwable的是在futureTask里面的catch-throwable语句
            // 那么这样这里的thrown是为null的，那么就不会跳出while循环，也就是说，这里的worker中的内部thread成员变量是不会变的，
            // 也就是说，不会去调用threadFactory创建新的线程的
            // 但是如果这里捕获到了throwable，thrown不为null，这时是会跳出while循环，此时completedAbruptly=true，发生了异常结束
            // 那么会创建一个runner为null的worker去替换这个出错的worker的，所以，即使出现了异常，pool的worker数量也是稳定的
            processWorkerExit(w, completedAbruptly);
        }
    }

    // Public constructors and methods

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @param workQueue       the queue to use for holding tasks before they are
     *                        executed.  This queue will hold only the {@code Runnable}
     *                        tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue} is null
     */
    public ConcurrentPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @param workQueue       the queue to use for holding tasks before they are
     *                        executed.  This queue will hold only the {@code Runnable}
     *                        tasks submitted by the {@code execute} method.
     * @param threadFactory   the factory to use when the executor
     *                        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} is null
     */
    public ConcurrentPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @param workQueue       the queue to use for holding tasks before they are
     *                        executed.  This queue will hold only the {@code Runnable}
     *                        tasks submitted by the {@code execute} method.
     * @param handler         the handler to use when execution is blocked
     *                        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code handler} is null
     */
    public ConcurrentPool(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            PoolRejectedHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize    the number of threads to keep in the pool, even
     *                        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *                        pool
     * @param keepAliveTime   when the number of threads is greater than
     *                        the core, this is the maximum time that excess idle threads
     *                        will wait for new tasks before terminating.
     * @param unit            the time unit for the {@code keepAliveTime} argument
     * @param workQueue       the queue to use for holding tasks before they are
     *                        executed.  This queue will hold only the {@code Runnable}
     *                        tasks submitted by the {@code execute} method.
     * @param threadFactory   the factory to use when the executor
     *                        creates a new thread
     * @param handler         the handler to use when execution is blocked
     *                        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} or {@code handler} is null
     */
    public ConcurrentPool(int corePoolSize,
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

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     * <p>
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution
     * @throws NullPointerException       if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        int c = ctl.get();
        // 如果worker数量 < core，那么addWorker创健新的线程执行runner
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            // 如果addWorker失败，获取状态再次判断
            c = ctl.get();
        }
        // 如果worker数量 > core，那么将任务放入任务队列
        // 如果pool已经调用了shutDown或者状态已经远不是running了，那么会继续addWorker，addWorker里面还是会判断状态，
        // 然后再决定是否reject拒绝，
        // 哈哈，上面是不是说了这种并发的设计其实就是同步状态的控制，哈哈，是吧？
        // 即使第一次判断是running并且同样offer成功了，也是有可能马上就被调用了shutDown的，所以还要判断recheck
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            // 如果不是running并且同步删除command成功，那么执行拒绝策略
            // 但是如果remove失败了，说明调用的可能是shutDown，已经被消费掉了
            // 但是调用的是shutDownNow，那么remove还是会失败或者成功的，因为这些操作remove，drainTo都是同步的
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                // 如果正好遇上仅有的一个worker它的poll发生timeout了，并且允许core核心timeout，需要addWorker
                // 不管add成功与否，确保有人消费队列任务，不要出现bug，哈哈
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {
            // 注意上面的判断，如果isRunning失败，那么会调用addWorker，里面会进行状态判断，哈哈，还是状态控制
            // 然后根绝结果选择是否拒绝
            // 但是如果pool是running的，但是offer的时候失败了，说明任务太多，满了
            // 这时会根据maxPoolSize判断是否创建多于core数量的新的worker去执行runner
            // 哈哈，基本这样了，主要的，核心的方法都搞定了
            // 下面的一些就不介绍了，很简单的
            // 还是那句话，从整体的设计上体会这些状态的控制
            // 很经典哦，哈哈
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

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(PoolRejectedHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(PoolRejectedHandler)
     */
    public PoolRejectedHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
        } else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty()) {
                    break;
                }
            }
        }
    }

    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
                addWorker(null, true);
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize) {
            addWorker(null, true);
        } else if (wc == 0) {
            addWorker(null, false);
        }
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     * else {@code false}
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *                                  and the current keep-alive time is not greater than zero
     * @since 1.6
     */
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

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *                                  less than or equal to zero, or
     *                                  less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize) {
            interruptIdleWorkers();
        }
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *             excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *                                  if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
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

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     * <p>
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /* Statistics */

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                    : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
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

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
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

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
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

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
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

    /* Extension hooks */

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     * <p>
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) {
    }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     * <p>
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     * <p>
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     * <p>
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     *          execution completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    protected void terminated() {
    }

    public static class CallerRunsPolicy extends BaojieRejectedHandler {
        public CallerRunsPolicy() {
            super("CallerRunsPolicy");
        }

        public void rejectedExecution(Runnable r, ConcurrentPool e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    public static class AbortPolicy extends BaojieRejectedHandler {

        public AbortPolicy() {
            super("AbortPolicy");
        }

        public void rejectedExecution(Runnable r, ConcurrentPool e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }

    }

    public static class DiscardPolicy extends BaojieRejectedHandler {
        public DiscardPolicy() {
            super("DiscardPolicy");
        }

        public void rejectedExecution(Runnable r, ConcurrentPool e) {
        }
    }

    public static class DiscardOldestPolicy extends BaojieRejectedHandler {

        public DiscardOldestPolicy() {
            super("DiscardOldestPolicy");
        }

        public void rejectedExecution(Runnable r, ConcurrentPool e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
