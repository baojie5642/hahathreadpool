package com.baojie.zk.example.concurrent.seda_refactor_01;

public interface TaskFuture<V> extends StageTask, FutureAdaptor<V> {

    @Override
    void task(Bus bus);

}
