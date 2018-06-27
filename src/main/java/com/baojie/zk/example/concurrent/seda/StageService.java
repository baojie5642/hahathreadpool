package com.baojie.zk.example.concurrent.seda;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface StageService<T> extends Fiber<T> {

    void shutdown();

    List<T> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    boolean submit(T task);

}
