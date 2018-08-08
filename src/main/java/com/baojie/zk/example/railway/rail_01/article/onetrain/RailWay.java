package com.baojie.zk.example.railway.rail_01.article.onetrain;

import java.util.concurrent.atomic.AtomicInteger;

public class RailWay {
    private final int stationCount = 2;
    private final Train train = new Train();
    private final AtomicInteger stationIndex = new AtomicInteger();

    public Train waitTrainOnStation(final int stationNo) {
        while (stationIndex.get() % stationCount != stationNo) {
            Thread.yield();
        }
        return train;
    }
    public void sendTrain() {
        stationIndex.getAndIncrement();
    }
}
