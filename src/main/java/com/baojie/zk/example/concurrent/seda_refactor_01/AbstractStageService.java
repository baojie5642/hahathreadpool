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
        localExec(ftask);
        return ftask;
    }

    public <T> FutureAdaptor<T> submit(StageTask task, T result) {
        if (task == null) throw new NullPointerException();
        StageFuture<T> ftask = newTaskFor(task, result);
        localExec(ftask);
        return ftask;
    }

    public <T> FutureAdaptor<T> submit(StageCall<T> task) {
        if (task == null) throw new NullPointerException();
        StageFuture<T> ftask = newTaskFor(task);
        localExec(ftask);
        return ftask;
    }

    // 修复submit中使用ThreadPoolExecutor.DiscardPolicy策略的bug
    // 在jdk的线程池中仍然存在这种问题
    private <T> void localExec(StageFuture<T> ftask) {
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
