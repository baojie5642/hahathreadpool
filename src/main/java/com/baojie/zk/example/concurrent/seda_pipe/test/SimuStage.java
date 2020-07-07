package com.baojie.zk.example.concurrent.seda_pipe.test;

import com.baojie.zk.example.concurrent.seda_pipe.Stage;
import com.baojie.zk.example.concurrent.seda_pipe.Stages;
import com.baojie.zk.example.concurrent.seda_pipe.bus.Bus;
import com.baojie.zk.example.concurrent.seda_pipe.task.Call;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SimuStage {

    public static final class Baojie implements Bus {
        public Baojie() {

        }

    }

    public static void main(String args[]) {


        Baojie bj = new Baojie();
        Stage stage = Stages.newFixed(1, "baojie-stage", bj);

        Future<Long> future = stage.submit(new Call<Long>() {
            @Override
            public Long call(Bus bus) {
                return 17621211981L;
            }
        });
        long test = -998L;
        try {
            test = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("test long=" + test);

        stage.shutdown();
    }

}
