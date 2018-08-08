package com.baojie.zk.example.concurrent.seda_refactor_01;

import java.util.concurrent.TimeUnit;

public interface StageService extends StageExecutor {

    void shutdown();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    boolean submit(StageTask task);

}
