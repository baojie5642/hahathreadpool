package com.baojie.zk.example.concurrent.seda_pipe.service;

import com.baojie.zk.example.concurrent.seda_pipe.future.StageFuture;
import com.baojie.zk.example.concurrent.seda_pipe.task.Call;
import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface StageService extends StageExecutor {

    void shutdown();

    List<Task> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> StageFuture<T> submit(Call<T> task);

    <T> StageFuture<T> submit(Task task, T result);

    StageFuture<?> submit(Task task);

}
