package com.baojie.zk;


import com.baojie.zk.example.concurrent.ConcurrentPoolTest;
import com.baojie.zk.example.concurrent.ScheduledPool;
import com.sun.tools.classfile.StackMapTable_attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Hello world!
 */
public class App {
    private final LinkedBlockingQueue<Object> oq = new LinkedBlockingQueue<>(32);
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public App() {

    }

    public void add() {
        oq.offer(new Object());
    }

    public List<Object> drainQueue() {
        BlockingQueue<Object> q = oq;
        ArrayList<Object> taskList = new ArrayList<Object>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Object r : q.toArray(new Object[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    public int size() {
        return oq.size();
    }

    public static void main(String[] args) {
        List<Object> list00 = new ArrayList<>(32);
        ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
        list00.add(new Object());
        list00.add(new Object());
        list00.add(new Object());
        list00.add(new Object());
        for (Object o : list00) {
            queue.offer(o);
        }
        list00.clear();
        System.out.println("list00 size=" + list00.size());

        System.out.println("queue size=" + queue.size());

        for (Object o00 = queue.poll(); o00 != null; o00 = queue.poll()) {
            System.out.println(o00);
        }

        final AtomicBoolean flag = new AtomicBoolean(true);
        App app = new App();
        for (int i = 0; i < 30; i++) {
            app.add();
        }
        List<Object> list = app.drainQueue();
        System.out.println(list.size());
        System.out.println(app.size());
        final ScheduledPool scheduledPool = new ScheduledPool(2);
        scheduledPool.setRemoveOnCancelPolicy(true);

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (flag.get()) {

                    try {
                        TimeUnit.SECONDS.sleep(1000);
                        log.info("……sleep work……");
                    } catch (InterruptedException i) {
                        log.info(i.toString(), i);
                    } catch (Throwable t) {
                        log.info(t.toString(), t);
                    }


                }
            }
        };

        final ScheduledFuture<?> scheduledFuture = scheduledPool.scheduleWithFixedDelay(runnable, 30, 6,
                TimeUnit.SECONDS);

        final Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(45, TimeUnit.SECONDS));
                flag.set(false);
                scheduledFuture.cancel(true);
                scheduledPool.purge();

                log.info("scheduled pool getActiveCount thread=" + scheduledPool.getActiveCount());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool getCompletedTaskCount thread=" + scheduledPool.getCompletedTaskCount());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool getRemoveOnCancelPolicy thread=" + scheduledPool.getRemoveOnCancelPolicy());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool isShutdown thread=" + scheduledPool.isShutdown());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool getActiveCount thread=" + scheduledPool.getActiveCount());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool getTaskCount thread=" + scheduledPool.getTaskCount());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                log.info("scheduled pool getQueue().size thread=" + scheduledPool.getQueue().size());
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS));
                flag.set(true);
                scheduledPool.scheduleWithFixedDelay(runnable, 30, 6, TimeUnit.SECONDS);


            }
        };

        Thread thread = new Thread(runnable1, "cancel scheduled future");
        thread.start();
    }
}
