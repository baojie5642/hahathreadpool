package com.baojie.zk.example.concurrent.seda_refactor_01.reject;

import com.baojie.zk.example.concurrent.seda_refactor_01.service.StageExecutor;
import com.baojie.zk.example.concurrent.seda_refactor_01.task.Task;

public interface StageRejected {

    void reject(Task task, StageExecutor executor, String reason);

}
