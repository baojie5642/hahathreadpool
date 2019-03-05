package com.baojie.zk.example.concurrent.seda_refactor_01;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface StageService extends StageExecutor {

    void shutdown();

    List<StageTask> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> Future<T> submit(StageCall<T> task);

    <T> Future<T> submit(StageTask task, T result);

    Future<?> submit(StageTask task);

}
