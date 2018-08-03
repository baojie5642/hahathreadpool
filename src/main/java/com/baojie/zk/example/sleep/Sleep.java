package com.baojie.zk.example.sleep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class Sleep {
    private static final Logger log = LoggerFactory.getLogger(Sleep.class);

    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final int FAST = (NCPU > 1) ? 1 << 6 : 0;
    public static final int MIDDLE = (NCPU > 1) ? 1 << 10 : 0;
    public static final int SLOW = (NCPU > 1) ? 1 << 16 : 0;

    static {
        log.info("System NCPU=" + NCPU);
        log.info("System FAST=" + FAST);
        log.info("System MIDDLE=" + MIDDLE);
        log.info("System SLOW=" + SLOW);
    }

    private Sleep() {
        throw new IllegalAccessError("init");
    }

    // 因为是sleep形式调用，会擦除掉中断状态
    public static void threadSleep(TimeUnit unit, long sleep) {
        Thread.yield();
        if (null == unit) {
            return;
        }
        try {
            unit.sleep(sleep);
        } catch (InterruptedException e) {
            log.error("occur error=" + e.toString() + ", interrupt status has been cleaned", e);
        } catch (Throwable te) {
            log.error("error=" + te.toString(), te);
        }
    }

    // 因为使用的locksupport，这种方式会忽略掉线程中断状态，并且立即返回，所以这个方法添加了一些安全性日志打印
    // 由于是先判断是否被中断，所以没有对状态进行擦除，locksupport也不会擦除
    // 如果在休眠期间检测到中断，能够快速响应
    public static void locksptSleep(TimeUnit unit, long sleep) {
        Thread.yield();
        if (null == unit) {
            return;
        }
        Thread t = Thread.currentThread();
        interrupted(t);
        park(unit, sleep);
    }

    private static void park(TimeUnit unit, long sleep) {
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(sleep, unit));
    }

    private static boolean interrupted(Thread t) {
        boolean isInterrupted = t.isInterrupted();
        if (isInterrupted) {
            String tn = t.getName();
            long tid = t.getId();
            log.error("interrupted before locksupport park , threadName=" + tn + ", threadId=" + tid);
        }
        return isInterrupted;
    }

}
