package com.baojie.zk.example.concurrent.seda_refactor_01.test;

import com.baojie.zk.example.concurrent.seda_refactor_01.Stage;
import com.baojie.zk.example.concurrent.seda_refactor_01.Stages;
import com.baojie.zk.example.concurrent.seda_refactor_01.bus.Bus;
import com.baojie.zk.example.concurrent.seda_refactor_01.future.StageFuture;
import com.baojie.zk.example.concurrent.seda_refactor_01.task.Task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class TestAll {

    public static void main(String args[]) {
        Stage stage = Stages.newNormal(5120, 5120, 3000,  "test", new World());
        AtomicInteger count = new AtomicInteger(0);
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(15, TimeUnit.SECONDS));
        StageFuture<AtomicInteger> future=null;
        for(int j=0;j<2048;j++){
             future = stage.submit(new LocalTask(count), count);
        }

        for (int i = 0; i < 1024; i++) {
            Futu futu = new Futu(future);
            Thread thread = new Thread(futu);
            thread.start();
        }
        if (future.hasSubmit()) {
            System.out.println("task has submit pool");
        }


        stage.shutdown();
        try {
            stage.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static final class World implements Bus {

    }

    public static final class LocalTask implements Task {

        private final AtomicInteger count;

        public LocalTask(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public void task(Bus bus) {
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS));
            count.incrementAndGet();
        }

    }


    public static final class Futu implements Runnable {

        private final Future<AtomicInteger> future;

        public Futu(Future<AtomicInteger> future) {
            this.future = future;
        }

        @Override
        public void run() {
            AtomicInteger count = null;
            System.out.println("start get");
            try {
                count = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (null != count) {
                System.out.println(count.get());
            }
            if (future instanceof StageFuture) {
                Throwable cause = ((StageFuture<?>) future).cause();

                if (null == cause) {
                    System.out.println("no error occur");
                }else {
                    cause.printStackTrace();
                }
            }


            System.out.println("finish get");
        }


    }


}
