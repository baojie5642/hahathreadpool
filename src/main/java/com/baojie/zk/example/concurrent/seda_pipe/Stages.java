package com.baojie.zk.example.concurrent.seda_pipe;

import com.baojie.zk.example.concurrent.seda_pipe.bus.Bus;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class Stages {

    public static Stage newFixed(int nThreads, String name, Bus bus) {
        return new Stage(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), name, bus);
    }

    public static Stage newSingle(String name, Bus bus) {
        return new Stage(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), name, bus);
    }

    public static Stage newCached(String name, Bus bus) {
        return new Stage(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), name, bus);
    }

    public static Stage newNormal(int core, int max, int qsize, String name, Bus bus) {
        return new Stage(core, max, 180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(qsize), name, bus);
    }

    public static Stage newDynamic(int core, int max, int keep, String name, Bus bus) {
        return new Stage(core, max, keep, TimeUnit.SECONDS, new SynchronousQueue<>(), name, bus);
    }

    private Stages() {

    }

}
