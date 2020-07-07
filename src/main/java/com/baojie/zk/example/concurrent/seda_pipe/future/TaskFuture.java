package com.baojie.zk.example.concurrent.seda_pipe.future;

import com.baojie.zk.example.concurrent.seda_pipe.bus.Bus;
import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

public interface TaskFuture<V> extends Task, StageFuture<V> {

    @Override
    void task(Bus bus);

}
