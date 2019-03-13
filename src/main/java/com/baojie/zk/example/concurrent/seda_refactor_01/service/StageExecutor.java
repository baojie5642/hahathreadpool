package com.baojie.zk.example.concurrent.seda_refactor_01.service;

import com.baojie.zk.example.concurrent.seda_refactor_01.task.Task;

public interface StageExecutor {

    boolean execute(Task task);

}
