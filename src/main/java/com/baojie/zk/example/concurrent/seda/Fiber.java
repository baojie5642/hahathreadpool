package com.baojie.zk.example.concurrent.seda;

public interface Fiber<T> {

    boolean execute(T t);

}
