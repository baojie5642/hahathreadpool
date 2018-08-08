package com.baojie.zk.example.concurrent.seda_refactor_01;

public interface StageExecutor {

    boolean execute(StageTask task);

}
