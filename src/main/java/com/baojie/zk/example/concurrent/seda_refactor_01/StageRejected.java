package com.baojie.zk.example.concurrent.seda_refactor_01;

public interface StageRejected {

    void reject(StageTask task, StageExecutor executor, String reason);

}
