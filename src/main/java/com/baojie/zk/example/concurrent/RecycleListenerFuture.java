package com.baojie.zk.example.concurrent;

import jdk.internal.misc.Unsafe;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public class RecycleListenerFuture<T> {

//    private volatile int state;
//    private static final int NEW = 0;
//    private static final int DOING = 1;
//    private static final int DONE = 2;
//    private static final int UNKNOW_ERR = 3;
//    private volatile Object outcome;
//    private volatile WaitNode waiters;
//
//    private RecycleListenerFuture(final Class<T> rc) {
//        this.state = NEW;
//    }
//
//    @SuppressWarnings({"rawtypes", "unchecked"})
//    public static <T> RecycleListenerFuture create(final Class<T> rc) {
//        if (null == rc) {
//            throw new NullPointerException("class null");
//        }
//        return new RecycleListenerFuture(rc);
//    }
//
//    public boolean isDone() {
//        return state != NEW;
//    }
//
//    public T get() throws InterruptedException, ExecutionException {
//        int s = state;
//        if (s <= DOING) {
//            s = awaitDone(false, 0L);
//        }
//        return report(s);
//    }
//
//    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//        if (unit == null) {
//            throw new NullPointerException("TimeUnit must not be null");
//        }
//        int s = state;
//        if (s <= DOING && (s = awaitDone(true, unit.toNanos(timeout))) <= DOING) {
//            throw new TimeoutException("future time out.");
//        }
//        return report(s);
//    }
//
//    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
//        final long deadline = timed ? System.nanoTime() + nanos : 0L;
//        WaitNode q = null;
//        boolean queued = false;
//        for (; ; ) {
//            if (Thread.interrupted()) {
//                removeWaiter(q);
//                throw new InterruptedException();
//            }
//            int s = state;
//            if (s > DOING) {
//                if (q != null) {
//                    q.thread = null;
//                }
//                return s;
//            } else if (s == DOING) {
//                Thread.yield();
//            } else if (q == null) {
//                q = new WaitNode();
//            } else if (!queued) {
//                queued = UnSafe.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
//            } else if (timed) {
//                nanos = deadline - System.nanoTime();
//                if (nanos <= 0L) {
//                    removeWaiter(q);
//                    return state;
//                }
//                LockSupport.parkNanos(this, nanos);
//            } else {
//                LockSupport.park(this);
//            }
//        }
//    }
//
//    private void removeWaiter(WaitNode node) {
//        if (null == node) {
//            return;
//        }
//        node.thread = null;
//        retry:
//        for (; ; ) {
//            for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
//                s = q.next;
//                if (q.thread != null) {
//                    pred = q;
//                } else if (pred != null) {
//                    pred.next = s;
//                    if (pred.thread == null) {
//                        continue retry;
//                    }
//                } else if (!UnSafe.compareAndSwapObject(this, waitersOffset, q, s)) {
//                    continue retry;
//                }
//            }
//            break;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private T report(int s) throws ExecutionException {
//        Object x = outcome;
//        if (s == DONE) {
//            return (T) x;
//        }
//        if (s >= UNKNOW_ERR) {
//            throw new IllegalStateException("future's State is UnknowError , occur Error");
//        }
//        throw new ExecutionException((Throwable) x);
//    }
//
//    public void set(final T object) {
//        setInner(object);
//    }
//
//    private void setInner(final T object) {
//        if (UnSafe.compareAndSwapInt(this, stateOffset, NEW, DOING)) {
//            outcome = object;
//            UnSafe.putOrderedInt(this, stateOffset, DONE);
//            finishCompletion();
//        }
//    }
//
//    private void finishCompletion() {
//        for (WaitNode q; (q = waiters) != null; ) {
//            if (UnSafe.compareAndSwapObject(this, waitersOffset, q, null)) {
//                for (; ; ) {
//                    Thread t = q.thread;
//                    if (t != null) {
//                        q.thread = null;
//                        LockSupport.unpark(t);
//                    }
//                    WaitNode next = q.next;
//                    if (next == null) {
//                        break;
//                    }
//                    q.next = null;
//                    q = next;
//                }
//                break;
//            }
//        }
//        done();
//    }
//
//    protected void done() {
//
//    }
//
//    private static final class WaitNode {
//        volatile Thread thread;
//        volatile WaitNode next;
//
//        WaitNode() {
//            thread = Thread.currentThread();
//        }
//    }
//
//    public int getState() {
//        return state;
//    }
//
//    public Object getOutcome() {
//        return outcome;
//    }
//
//    public void reset() {
//        if (NEW == state) {
//            return;
//        } else if (DONE == state) {
//            innerReset();
//        } else if (DOING == state) {
//            throw new IllegalStateException("State must not be Completing");
//        } else if (state >= UNKNOW_ERR) {
//            throw new IllegalStateException("State must not be UnknowError");
//        }
//    }
//
//    private void innerReset() {
//        finishCompletion();
//        realReset();
//    }
//
//    private void realReset() {
//        boolean casSetSuccess = true;
//        resetOutCome();
//        if (state == NEW) {
//            return;
//        } else {
//            casSetSuccess = UnSafe.compareAndSwapInt(this, stateOffset, DONE, NEW);
//            logDebug(casSetSuccess);
//        }
//    }
//
//    private void logDebug(final boolean casSetSuccess) {
//        if (casSetSuccess) {
//            return;
//        } else {
//        }
//    }
//
//    private void resetOutCome() {
//        if (null != outcome) {
//            outcome = null;
//        }
//    }
//
//    private static final Unsafe UnSafe;
//    private static final long stateOffset;
//    private static final long waitersOffset;
//
//    static {
//        try {
//            UnSafe = HaUnsafe.getUnsafe();
//            Class<?> k = RecycleListenerFuture.class;
//            stateOffset = UnSafe.objectFieldOffset(k.getDeclaredField("state"));
//            waitersOffset = UnSafe.objectFieldOffset(k.getDeclaredField("waiters"));
//        } catch (Exception e) {
//            throw new Error(e);
//        }
//    }

}
