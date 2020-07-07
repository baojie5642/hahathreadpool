package com.baojie.zk.example.concurrent.seda_pipe.future;

import com.baojie.zk.example.concurrent.seda_pipe.bus.Bus;
import com.baojie.zk.example.concurrent.seda_pipe.task.Call;
import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public class FutureAdaptor<V> implements TaskFuture<V> {

    private volatile int state;
    private static final int NEW = 0;
    private static final int COMPLETING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED = 6;

    private volatile boolean submit = false;

    /**
     * The underlying callable; nulled out after running
     */
    private Call<V> callable;
    /**
     * The result to return or exception to throw from get()
     */
    private Object outcome; // non-volatile, protected by state reads/writes
    /**
     * The thread running the callable; CASed during run()
     */
    private volatile Thread runner;
    /**
     * Treiber stack of waiting threads
     */
    private volatile WaitNode waiters;

    /**
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) {
            return (V) x;
        }
        if (s >= CANCELLED) {
            throw new CancellationException();
        }
        throw new ExecutionException((Throwable) x);
    }

    public FutureAdaptor(Call<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    public FutureAdaptor(Task task, V result) {
        this.callable = new TaskAdapter(task, result);
        this.state = NEW;       // ensure visibility of callable
    }

    private static final class TaskAdapter<T> implements Call<T> {

        final Task task;
        final T result;

        TaskAdapter(Task task, T result) {
            this.task = task;
            this.result = result;
        }

        @Override
        public T call(Bus bus) {
            task.task(bus);
            return result;
        }

    }

    protected final void setSubmit(boolean suc) {
        this.submit = suc;
    }

    @Override
    public final boolean hasSubmit() {
        final boolean suc = submit;
        return suc;
    }

    @Override
    public Throwable cause() {
        Object result = this.outcome;
        if (null == result) {
            return null;
        } else {
            if (result instanceof Throwable) {
                return (Throwable) result;
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public boolean cancel(boolean may) {
        if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, may ? INTERRUPTING : CANCELLED))) {
            return false;
        }
        try {
            if (may) {
                interruptIfRunning();
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    private void interruptIfRunning() {
        try {
            Thread t = runner;
            if (t != null) {
                t.interrupt();
            }
        } finally { // final state
            UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING) {
            s = awaitDone(false, 0L);
        }
        return report(s);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        int s = state;
        if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
            throw new TimeoutException();
        }
        return report(s);
    }

    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() {
    }

    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }

    @Override
    public void task(Bus bus) {
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) {
            return;
        }
        try {
            Call<V> c = callable;
            if (!hasSubmit()) {
                setException(new IllegalStateException("submit stage fail"));
            } else if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call(bus);
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran) {
                    set(result);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING) {
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt
        }

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    private static final class WaitNode {

        volatile Thread thread;
        volatile WaitNode next;

        WaitNode() {
            thread = Thread.currentThread();
        }
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING;
        try {
            outter:
            for (WaitNode q; (q = waiters) != null; ) {
                if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                    inner:
                    for (; ; ) {
                        Thread t = q.thread;
                        if (t != null) {
                            q.thread = null;
                            LockSupport.unpark(t);
                        }
                        WaitNode next = q.next;
                        if (next == null) {
                            break inner;
                        }
                        q.next = null; // unlink to help gc
                        q = next;
                    }
                    break outter;
                }
            }
        } finally {
            try {
                done();
            } finally {
                callable = null;        // to reduce footprint
            }
        }
    }

    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (; ; ) {
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }
            int s = state;
            if (s > COMPLETING) {
                if (q != null) {
                    q.thread = null;
                }
                return s;
            } else if (s == COMPLETING) {
                Thread.yield();
            } else if (q == null) {
                q = new WaitNode();
            } else if (!queued) {
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            } else {
                LockSupport.park(this);
            }
        }
    }

    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (; ; ) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null) {
                        pred = q;
                    } else if (pred != null) {
                        pred.next = s;
                        // 竞争重试
                        if (pred.thread == null) {
                            continue retry;
                        }
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) {
                        continue retry;
                    }
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;

    static {
        try {
            UNSAFE = LocalUnsafe.getUnsafe();
            Class<?> k = FutureAdaptor.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
