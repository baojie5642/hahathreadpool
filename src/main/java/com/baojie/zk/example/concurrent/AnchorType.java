package com.baojie.zk.example.concurrent;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AnchorType<E> {
    /**
     * Refer to {@link LockFreeDeque} for explanation of these three states.
     * Enumeration is inconvenient to be operated as integer and not used here.
     */
    private static final int STABLE = 0;

    /**
     * This is a flag variable to indicate the status of deque. It can be one of
     * the STABLE, RPUSH, LPUSH. This field has default access privilege since
     * Deque will visit it directly.
     */
    volatile int status;

    /**
     * This variable will be used to update <code>status</code> in an atomic
     * approach.
     */
    private static final AtomicIntegerFieldUpdater<AnchorType> STATUS_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(AnchorType.class, "status");

    /**
     * Right or tail node in the deque.
     */
    DequeNode<E> right;
    /**
     * Left or head node in the deque.
     */
    DequeNode<E> left;

    /**
     * Number of elements inside the Deque.
     */
    int numElements;

    /**
     * Try to CAS <code>status</code> from oldStatus to STABLE. Do nothing if
     * CAS fails.
     *
     * @param oldStatus old status expected
     */
    public void stableStatus(int oldStatus) {
        /**
         * Multiple threads can try to stable deque at the same time. It's not
         * important that which thread did the actual work. We need CAS to
         * ensure atomicity. If CAS fails, it means other thread did the work.
         * This thread can return without any further action.
         */
        STATUS_UPDATER.compareAndSet(this, oldStatus, STABLE);
    }

    /**
     * default constructor.
     */
    public AnchorType() {
    }

    /**
     * @param r  right node
     * @param l  left node
     * @param st status
     * @param ne number of element
     */
    public AnchorType(DequeNode<E> r, DequeNode<E> l, int st, int ne) {
        setup(r, l, st, ne);
    }

    /**
     * Setup the anchor with the input parameters.
     *
     * @param r  right node
     * @param l  left node
     * @param st status of deque
     * @param ne number of element
     */
    void setup(DequeNode<E> r, DequeNode<E> l, int st, int ne) {
        right = r;
        left = l;
        status = st;
        numElements = ne;
    }

    /**
     * Return size of Deque.
     *
     * @return size of Deque
     */
    public int getSize() {
        return this.numElements;
    }
}