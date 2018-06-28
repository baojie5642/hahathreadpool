package com.baojie.zk.example.concurrent.seda_refactor;

public interface Stage_Executor {

    boolean execute(Stage_Task task);

}
