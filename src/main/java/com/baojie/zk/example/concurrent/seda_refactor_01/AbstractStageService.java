package com.baojie.zk.example.concurrent.seda_refactor_01;

public abstract class AbstractStageService implements StageService {

    public final boolean submit(StageTask task) {
        if (task == null) {
            return false;
        } else {
            return execute(task);
        }
    }

}
