package com.baojie.zk.example.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class StampTest {

    public static void main(String[] args) throws InterruptedException {

        final HaStampLock lock = new HaStampLock();

        new Thread() {

            public void run() {

                long readLong = lock.writeLock();

                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(1, TimeUnit.HOURS));

                lock.unlockWrite(readLong);

            }

        }.start();

        Thread.sleep(10000);
        List<Thread> threadslist=new ArrayList<>();
        Thread ttt = null;
        for (int i = 0; i < 32; ++i) {

            ttt = new Thread(new OccupiedCPUReadThread(lock));
            ttt.start();
            threadslist.add(ttt);
        }
        Thread.sleep(30000);

        for(Thread t:threadslist){
            t.interrupt();
        }

    }

    private static class OccupiedCPUReadThread implements Runnable {

        private HaStampLock lock;

        public OccupiedCPUReadThread(HaStampLock lock) {

            this.lock = lock;

        }

        public void run() {

            Thread.currentThread().interrupt();

            long lockr = 0;
            try {
                lockr = lock.readLock();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(Thread.currentThread().getName() + " get read lock=" + lockr);

            lock.unlockRead(lockr);

        }

    }

}
