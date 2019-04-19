package com.baojie.zk.example.concurrent.loopwork;

import com.baojie.zk.example.concurrent.TFactory;
import com.baojie.zk.example.concurrent.memory.MemRun;
import com.baojie.zk.example.util.LoggerMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class DevilWork {
    private static final Logger log = LoggerMaker.logger();
    private static final int N_CPU = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_WORK_NUM = 4;
    private static final int SIGNAL_NUM = 8;
    // 整个系统停止的标志
    private final AtomicBoolean worldStop = new AtomicBoolean(false);
    private final ReentrantLock mainLock = new ReentrantLock();
    private final Semaphore signal = new Semaphore(SIGNAL_NUM);
    private final ConcurrentHashMap<Long, WorkStop> workers = new ConcurrentHashMap<>();
    private final AtomicLong key = new AtomicLong(0);

    // 线程池使用具有弹性的队列
    private final ThreadPoolExecutor pool =
            new ThreadPoolExecutor(N_CPU * 2, 256, 180, TimeUnit.MINUTES, new SynchronousQueue<>());
    // 方法是同步方法
    // 可能每次调用的该方法的客户端不同
    // 所以暂时不考虑使用线程池共用队列的结合模式
    // 直接使用开启多个线程的形式，但是
    // 为了提高性能，要求线程复用，所以还是使用线程池设计如下


    // 对外提供整个系统的停止操作
    // 同时，停止所有当前正在执行的update操作
    // 这是一个工具类
    // 如果整个系统关闭
    // 千万别忘记调用此方法关闭
    public void stop() {
        if (worldStop.get()) {
            return;
        } else {
            // 同步控制停止
            // 只有一个线程停止成功，其余操作直接返回
            // 下面的代码中，仅仅是get操作，并不需要通过获取锁来操作控制系统停止
            // 获取锁，仅仅是为了操作缓存达到安全停止当前正在执行的任务的目的
            if (worldStop.compareAndSet(false, true)) {
                try {
                    releaseSignal();
                    stopCurrentWorker();
                } finally {
                    shutDownPool();
                }
            }
        }
    }

    // worldStop已经被设置为true，不用再设置了
    // 释放信号量
    private void releaseSignal() {
        for (int i = 0; i < SIGNAL_NUM; i++) {
            signal.release(1);
        }
    }

    // 停止当前所有的任务执行
    private void stopCurrentWorker() {
        final ReentrantLock lock = mainLock;
        lock.lock();
        try {
            try {
                for (WorkStop ws : workers.values()) {
                    ws.stop();
                }
            } finally {
                workers.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    // 停止线程池
    // 总共等待大约1秒，1秒延迟
    private void shutDownPool() {
        for (int i = 0; i < 300; i++) {
            pool.shutdown();
            if (!pool.isShutdown()) {
                Thread.yield();
                // 休眠3毫秒
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.MILLISECONDS));
            }
        }
        pool.shutdownNow();
    }

    // 1.要更新的数据list，2.此次同步执行需要的线程个数,3.需要阻塞等待获取系统资源的超时时间
    public void work(List<String> workerIds, int workThreadNum, int wait4Signal) {
        if (worldStop.get()) {
            return;
        }
        if (null == workerIds) {
            return;
        }
        int size = workerIds.size();
        if (0 >= size) {
            return;
        }
        int wtn = workThreadNum;
        if (wtn <= 0 || wtn >= N_CPU * 2) {
            wtn = DEFAULT_WORK_NUM;
        }
        // 没有获取到批次的信号量
        // 直接返回
        if (!acquire(wait4Signal)) {
            return;
        }
        try {
            doUpdateWork(workerIds, wtn);
        } finally {
            // 任务执行完成，在释放信号量
            signal.release(1);
        }
    }

    // 等待获取系统资源（执行的批次）的时间，单位秒
    // 如果在指定时间内还没获取到系统资源，直接返回失败
    // 参数大小与等待的时间单位，依照业务需求调整
    private boolean acquire(int wait4Signal) {
        boolean suc = false;
        try {
            suc = signal.tryAcquire(1, wait4Signal, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        } catch (Throwable te) {

        }
        // 整个系统停止，返回false
        if (worldStop.get()) {
            // 如果在获取到系统资源后，整个系统停止了
            // 同样释放信号量，虽然可以不释放
            if (suc) {
                signal.release(1);
            }
            return false;
        } else {
            return suc;
        }
    }

    private void doUpdateWork(List<String> workerIds, int wtn) {
        final ConcurrentLinkedQueue<String> queue = shift2Queue(workerIds);
        final CountDownLatch latch = new CountDownLatch(wtn);
        final List<Future<?>> fs = new ArrayList<>(wtn);
        final AtomicBoolean stop = new AtomicBoolean(false);
        final long k = key.getAndIncrement();
        try {
            // 先将将要执行启动的线程任务添加到缓存,防止后续的系统停止出现问题
            // 这里要获取锁进行添加操作，防止此时的系统停止操作出错
            fill4Stop(k, latch, fs, wtn, stop);
            // 放入缓存后，启动线程任务
            submit(queue, fs, latch, wtn, stop);
            // 等待任务执行结束
            wait4Work(queue, fs, latch, stop);
        } finally {
            // 执行到finally时，说明之前的任务处理完成或者被停止
            // 此时可以直接remove，无需获取锁
            // 这个缓存只是为了停止整个系统时，仍然有同步的调用方法执行
            // 这样会阻塞系统停止
            // 仅此一个作用，暂时没有其他作用
            workers.remove(k);
        }
    }


    private void fill4Stop(long k, CountDownLatch latch, List<Future<?>> fs, int wtn, AtomicBoolean stop) {
        WorkStop ws = new WorkStop(latch, fs, wtn, stop);
        // 在装填缓存的时候没有判断worldStop
        // 因为在任务的执行方法runner中已经判断
        final ReentrantLock lock = mainLock;
        lock.lock();
        try {
            workers.putIfAbsent(k, ws);
        } finally {
            lock.unlock();
        }
    }

    private void submit(ConcurrentLinkedQueue<String> queue, List<Future<?>> fs, CountDownLatch latch, int wtn,
            AtomicBoolean stop) {
        for (int i = 0; i < wtn; i++) {
            if (worldStop.get()) {
                latch.countDown();
            } else {
                UpdateWorker uw = new UpdateWorker(latch, queue, stop);
                Future<?> f = submit2Pool(uw);
                if (null != f && !f.isDone()) {
                    fs.add(f);
                } else {
                    latch.countDown();
                }
            }
        }
    }

    private Future<?> submit2Pool(Runnable runnable) {
        // 为了防止被拒绝的情况发生，捕获异常
        try {
            return pool.submit(runnable);
        } catch (Throwable te) {

        }
        return null;
    }

    private void wait4Work(ConcurrentLinkedQueue<String> queue, List<Future<?>> fs, CountDownLatch latch,
            AtomicBoolean stop) {
        boolean suc = false;
        try {
            // 等待12个小时，如果还没执行完成，直接报错，
            // 异常请自行处理
            // 如果12小时还没有执行完成，该如何处理？
            // 参照需求来设计
            suc = latch.await(12, TimeUnit.HOURS);
        } catch (Throwable te) {

        } finally {
            // 没执行完成，直接取消执行
            // 并且清空队列，释放内存
            // 单纯的例子，具体实现请参考业务需求
            try {
                if (!suc) {
                    stop.set(true);
                }
            } finally {
                cleanAndStop(fs, queue);
            }
        }
    }

    private void cleanAndStop(List<Future<?>> fs, ConcurrentLinkedQueue<String> queue) {
        try {
            for (Future<?> f : fs) {
                f.cancel(true);
            }
        } finally {
            queue.clear();
        }
    }

    private ConcurrentLinkedQueue<String> shift2Queue(List<String> list) {
        final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        try {
            for (String id : list) {
                queue.offer(id);
            }
        } finally {
            list.clear();
        }
        return queue;
    }

    private final class WorkStop {
        private final CountDownLatch latch;
        private final List<Future<?>> fs;
        private final int count;
        private final AtomicBoolean stop;

        public WorkStop(CountDownLatch latch, List<Future<?>> fs, int count, AtomicBoolean stop) {
            this.latch = latch;
            this.fs = fs;
            this.count = count;
            this.stop = stop;
        }

        public void stop() {
            try {
                stop.set(true);
                futureStop();
            } finally {
                countDown();
            }
        }

        private void futureStop() {
            for (Future<?> f : fs) {
                f.cancel(true);
            }
        }

        private void countDown() {
            for (int i = 0; i < count; i++) {
                latch.countDown();
            }
        }

    }

    private final class UpdateWorker implements Runnable {

        private final ConcurrentLinkedQueue<String> queue;
        private final CountDownLatch latch;
        private final AtomicBoolean stop;

        public UpdateWorker(CountDownLatch latch, ConcurrentLinkedQueue<String> queue, AtomicBoolean stop) {
            this.latch = latch;
            this.queue = queue;
            this.stop = stop;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    if (worldStop.get()) {
                        break;
                    }
                    if (stop.get()) {
                        break;
                    } else {
                        String id = queue.poll();
                        if (null == id) {
                            break;
                        } else {
                            update(id);
                        }
                    }
                }
            } finally {
                release();
            }
        }

        private void release() {
            try {
                latch.countDown();
            } finally {
                // 单独取消此方法的执行或者是
                // 整个系统已经停止，那么都要清空队列
                if (stop.get() || worldStop.get()) {
                    queue.clear();
                }
            }
        }

        private void update(String id) {
            try {

            } catch (Throwable te) {

            }
        }

    }

    public static void main(String args[]) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 6, 180, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
                , TFactory.create("pool_test"));

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                throw new RuntimeException();
            }
        };

        executor.prestartCoreThread();
        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.execute(runnable);

        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.execute(runnable);

        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.execute(runnable);
        try {
            TimeUnit.SECONDS.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.execute(runnable);

    }


}
