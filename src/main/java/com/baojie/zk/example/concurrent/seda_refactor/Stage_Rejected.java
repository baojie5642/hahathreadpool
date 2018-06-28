package com.baojie.zk.example.concurrent.seda_refactor;

public interface Stage_Rejected {

    void reject(Stage_Task task, Stage_Executor executor,String reason);

}
