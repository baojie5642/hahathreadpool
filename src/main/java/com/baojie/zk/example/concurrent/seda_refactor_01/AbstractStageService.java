package com.baojie.zk.example.concurrent.seda_refactor_01;

public abstract class AbstractStageService implements StageService {

    protected <T> StageFuture<T> newTaskFor(StageTask task, T value) {
        return new StageFuture(task, value);
    }

    protected <T> StageFuture<T> newTaskFor(StageCall<T> callable) {
        return new StageFuture(callable);
    }

    public FutureAdaptor<?> submit(StageTask task) {
        if (task == null) throw new NullPointerException();
        StageFuture<Void> ftask = newTaskFor(task, null);
        boolean suc = execute(ftask);
        ftask.setSubmit(suc);
        return ftask;
    }

    public <T> FutureAdaptor<T> submit(StageTask task, T result) {
        if (task == null) throw new NullPointerException();
        StageFuture<T> ftask = newTaskFor(task, result);
        boolean suc = execute(ftask);
        ftask.setSubmit(suc);
        return ftask;
    }

    public <T> FutureAdaptor<T> submit(StageCall<T> task) {
        if (task == null) throw new NullPointerException();
        StageFuture<T> ftask = newTaskFor(task);
        boolean suc = execute(ftask);
        ftask.setSubmit(suc);
        return ftask;
    }

}
