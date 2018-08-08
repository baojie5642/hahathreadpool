package com.baojie.zk.example.railway.rail_01.article.onetrain;

public class RailWayTest {
    public static void main(String[] args) {
        new RailWayTest().testRailWay();
    }

    public void testRailWay() {
        final RailWay railway = new RailWay();

        final long n = 20000000000l;

        new Thread() {
            long lastValue = 0;

            @Override
            public void run() {

                while (lastValue < n) {
                    Train train = railway.waitTrainOnStation(1);
                    int count = train.goodsCount();
                    for (int i = 0; i < count; i++) {
                        lastValue = train.getGoods(i);
                    }
                    railway.sendTrain();
                }
            }
        }.start();

        final long start = System.nanoTime();

        long i = 0;
        while (i < n) {
            Train train = railway.waitTrainOnStation(0);
            int capacity = train.getCapacity();
            for (int j = 0; j < capacity; j++) {
                train.addGoods((int)i++);
            }
            railway.sendTrain();

            if (i % 1000000 == 0) {
                final long duration = System.nanoTime() - start;

                final long ops = (i * 1000L * 1000L * 1000L) / duration;
                System.out.format("ops/sec       = %,d\n", ops);
                System.out.format("trains/sec    = %,d\n", ops / Train.CAPACITY);
                System.out.format("latency nanos = %.3f%n\n", duration / (float)(i) * (float) Train.CAPACITY);

            }
        }
    }

}
