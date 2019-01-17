package com.baojie.zk.example.concurrent.seda_refactor_01;

public interface StageTask {

    // 执行每个线程对应的业务逻辑
    void task(Bus bus);

}
