package com.baojie.zk.example.fiber;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;


import java.util.Arrays;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import java.util.concurrent.atomic.AtomicInteger;


public class MyFiber {

    public void printer(Channel<Integer> in) throws SuspendExecution, InterruptedException {
        Integer v;
        while ((v = in.receive()) != null) {
            System.out.println(v);
        }
    }
    final int[] cache = new int[]{0};
    private static Random random = new Random();
    private static final int NUMBER_COUNT = 1000;
    private static final int RUNS = 4;
    private static final int BUFFER = 1000; // = 0 unbufferd, > 0 buffered ; < 0 unlimited

    private static void numberSort() {
        int[] nums = new int[NUMBER_COUNT];
        for (int i = 0; i < NUMBER_COUNT; i++)
            nums[i] = random.nextInt(NUMBER_COUNT);
        Arrays.sort(nums);
    }

    static void skynet(Channel<Long> c, long num, int size, int div) throws SuspendExecution, InterruptedException {
        if (size == 1) {
            c.send(num);
            return;
        }
        //加入排序逻辑
        numberSort();
        Channel<Long> rc = Channels.newChannel(BUFFER);
        long sum = 0L;
        for (int i = 0; i < div; i++) {
            long subNum = num + i * (size / div);
            new Fiber(() -> skynet(rc, subNum, size / div, div)).start();
        }
        for (int i = 0; i < div; i++)
            sum += rc.receive();
        c.send(sum);
    }

    public void print() {
        //定义两个Channel
        final Channel<Integer> naturals = Channels.newChannel(-1);
        final Channel<Integer> squares = Channels.newChannel(-1);

        //运行两个Fiber实现.
        new Fiber(() -> {
            for (int i = 0; i < 10; i++) naturals.send(i);
            naturals.close();
        }).start();
        new Fiber(() -> {
            Integer v;
            while ((v = naturals.receive()) != null) squares.send(v * v);
            squares.close();
        }).start();
        try {
            printer(squares);
        } catch (SuspendExecution suspendExecution) {
            suspendExecution.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createMilli() {



        int FiberNumber = 1_000_00;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < FiberNumber; i++) {
            new Fiber(() -> {


                Strand.sleep(10000);

                counter.incrementAndGet();
                if (counter.get() == FiberNumber) {
                    System.out.println("done");
                    System.out.println("cache array=" + cache[0]);
                }

            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("done");
    }


    public static void main(String[] args) throws InterruptedException, SuspendExecution {
        MyFiber mb=new MyFiber();
        mb.createMilli();
//        for (int i = 0; i < RUNS; i++) {
//            long start = System.nanoTime();
//
//            Channel<Long> c = Channels.newChannel(BUFFER);
//            new Fiber(() -> skynet(c, 0, 1_000_000, 10)).start();
//            long result = c.receive();
//
//            long elapsed = (System.nanoTime() - start) / 1_000_000;
//            System.out.println((i + 1) + ": " + result + " (" + elapsed + " ms)");
//        }
    }
}



