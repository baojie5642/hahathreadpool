package com.baojie.zk.example.concurrent.seda_refactor_01.task;

import com.baojie.zk.example.concurrent.seda_refactor_01.bus.Bus;

public interface Task {

    // 执行每个线程对应的业务逻辑
    void task(Bus bus);

}
