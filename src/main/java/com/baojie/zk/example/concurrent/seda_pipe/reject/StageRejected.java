package com.baojie.zk.example.concurrent.seda_pipe.reject;

import com.baojie.zk.example.concurrent.seda_pipe.service.StageExecutor;
import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

public interface StageRejected {

    void reject(Task task, StageExecutor executor, String reason);

}
