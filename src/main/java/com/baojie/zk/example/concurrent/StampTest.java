package com.baojie.zk.example.concurrent;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;

public class StampTest {

    public static void main(String[] args) throws InterruptedException {

        final HaStampLock lock = new HaStampLock();

        new Thread() {

            public void run() {

                long readLong = lock.writeLock();

                LockSupport.parkNanos(6100000000L);

                lock.unlockWrite(readLong);

            }

        }.start();

        Thread.sleep(100);

        for (int i = 0; i < 3; ++i)

            new Thread(new OccupiedCPUReadThread(lock)).start();

    }

    private static class OccupiedCPUReadThread implements Runnable {

        private HaStampLock lock;

        public OccupiedCPUReadThread(HaStampLock lock) {

            this.lock = lock;

        }

        public void run() {

            Thread.currentThread().interrupt();

            long lockr = lock.readLock();

            System.out.println(Thread.currentThread().getName() + " get read lock");

            lock.unlockRead(lockr);

        }

    }

}
