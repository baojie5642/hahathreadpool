package com.baojie.zk.example.concurrent.seda_refactor_01;

import io.netty.handler.timeout.ReadTimeoutException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class SimuStage {

    public static final class Baojie implements Bus {
        public Baojie() {

        }

    }

    public static void main(String args[]) {


        Baojie bj = new Baojie();
        Stage<Baojie> stage = new Stage<>(1, 1, 180, "baojie-stage", bj);
        Baojie bus = stage.getBus();
        if (bus instanceof Bus) {
            System.out.println("ok");
        }
        Future<Long> future = stage.submit(new StageCall<Long>() {
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
