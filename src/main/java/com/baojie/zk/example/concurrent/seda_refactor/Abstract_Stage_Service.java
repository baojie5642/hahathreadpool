package com.baojie.zk.example.concurrent.seda_refactor;

public abstract class Abstract_Stage_Service implements Stage_Service {

    public boolean submit(Stage_Task task) {
        if (task == null) {
            return false;
        } else {
            return execute(task);
        }
    }

}
