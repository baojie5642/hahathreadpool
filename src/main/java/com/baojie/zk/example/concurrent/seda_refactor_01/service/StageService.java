package com.baojie.zk.example.concurrent.seda_refactor_01.service;

import com.baojie.zk.example.concurrent.seda_refactor_01.future.StageFuture;
import com.baojie.zk.example.concurrent.seda_refactor_01.task.Call;
import com.baojie.zk.example.concurrent.seda_refactor_01.task.Task;

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
