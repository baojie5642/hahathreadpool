package com.baojie.zk.example.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public class DequeNode<E> {
    /**
     * Data on the node.
     */
    final E data;
    /**
     * Right pointer.
     */
    AtomicReference<DequeNode<E>> right;
    /**
     * Left Pointer.
     */
    AtomicReference<DequeNode<E>> left;

    /**
     * @param d default value of element
     */
    public DequeNode(E d) {
        this.data = d;
        this.right = new AtomicReference<DequeNode<E>>();
        this.left = new AtomicReference<DequeNode<E>>();
    }

    /**
     * @param r right node of node
     */
    public void setRight(DequeNode<E> r) {
        this.right.set(r);
    }

    /**
     * @param l left node of node
     */
    public void setLeft(DequeNode<E> l) {
        this.left.set(l);
    }

}
