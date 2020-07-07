package com.baojie.zk.example.concurrent.seda_pipe.test;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class TestLocal {

    public static void main(String args[]){
        ThreadPoolExecutor executor= new ThreadPoolExecutor(1,1,300,TimeUnit.SECONDS,new SynchronousQueue<>());
        executor.setRejectedExecutionHandler(new  ThreadPoolExecutor.DiscardPolicy());
        Future<?> future=null;
        for(int i=0;i<2;i++){
            future=executor.submit(new Fufu());
        }
        for (int i = 0; i < 1; i++) {
            GetFu futu = new GetFu(future);
            Thread thread = new Thread(futu);
            thread.start();
        }



        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }







    }

    public static final class Fufu implements Runnable{

        @Override
        public void run(){
            LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS));
        }
    }
    public static final class GetFu implements Runnable{

        private final Future<?> future;

        public GetFu(Future<?> future){
            this.future=future;
        }

        @Override
        public void run(){
            System.out.println("start get");
            try {
                 future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            System.out.println("finish get");
        }
    }

}
