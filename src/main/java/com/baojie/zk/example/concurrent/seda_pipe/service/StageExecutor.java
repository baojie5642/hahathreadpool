package com.baojie.zk.example.concurrent.seda_pipe.service;

import com.baojie.zk.example.concurrent.seda_pipe.task.Task;

public interface StageExecutor {

    boolean execute(Task task);

}
