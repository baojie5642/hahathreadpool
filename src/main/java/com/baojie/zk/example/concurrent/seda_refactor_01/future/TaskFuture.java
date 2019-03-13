package com.baojie.zk.example.concurrent.seda_refactor_01.future;

import com.baojie.zk.example.concurrent.seda_refactor_01.bus.Bus;
import com.baojie.zk.example.concurrent.seda_refactor_01.task.Task;

public interface TaskFuture<V> extends Task, StageFuture<V> {

    @Override
    void task(Bus bus);

}
