package com.baojie.zk.example.concurrent.seda_refactor;

import java.util.concurrent.*;

public interface Stage_Service extends Stage_Executor {

    void shutdown();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    boolean submit(Stage_Task task);

}
