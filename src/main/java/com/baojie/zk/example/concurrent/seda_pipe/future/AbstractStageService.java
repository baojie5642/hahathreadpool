package com.baojie.zk.example.concurrent.seda_pipe.future;

import com.baojie.zk.example.concurrent.seda_pipe.service.StageService;
import com.baojie.zk.example.concurrent.seda_pipe.task.Call;
import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

public abstract class AbstractStageService implements StageService {

    protected <T> FutureAdaptor<T> newTaskFor(Task task, T value) {
        return new FutureAdaptor(task, value);
    }

    protected <T> FutureAdaptor<T> newTaskFor(Call<T> call) {
        return new FutureAdaptor(call);
    }

    public StageFuture<?> submit(Task task) {
        if (task == null) throw new NullPointerException();
        FutureAdaptor<Void> ftask = newTaskFor(task, null);
        localExec(ftask);
        return ftask;
    }

    public <T> StageFuture<T> submit(Task task, T result) {
        if (task == null) throw new NullPointerException();
        FutureAdaptor<T> ftask = newTaskFor(task, result);
        localExec(ftask);
        return ftask;
    }

    public <T> StageFuture<T> submit(Call<T> task) {
        if (task == null) throw new NullPointerException();
        FutureAdaptor<T> ftask = newTaskFor(task);
        localExec(ftask);
        return ftask;
    }

    // 修复submit中使用ThreadPoolExecutor.DiscardPolicy策略的bug
    // 在jdk的线程池中仍然存在这种问题
    private <T> void localExec(FutureAdaptor<T> ftask) {
        boolean suc = false;
        try {
            suc = execute(ftask);
            if (!suc) {
                ftask.setException(new IllegalStateException("submit stage fail"));
            }
        } finally {
            ftask.setSubmit(suc);
        }
    }

}
