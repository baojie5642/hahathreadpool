package com.baojie.zk.example.concurrent.filecount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MyCount {
    private static final Logger log = LoggerFactory.getLogger(MyCount.class);
    private final ThreadPoolExecutor executor;

    private final int threadNum;


    public MyCount(int threadNum) {
        this.threadNum = threadNum;
        this.executor = new ThreadPoolExecutor(threadNum, 2 * threadNum, 180, TimeUnit.SECONDS, new
                SynchronousQueue<>());
    }

    public long sum(String dir) {
        if (null == dir) {
            return -1;
        }
        File f = new File(dir);
        f.setWritable(true);
        if (f.isFile()) {
            return 1L;
        }
        if (!f.isDirectory()) {
            return -1L;
        }
        final List<DirOnly> dirOnlyList = new CopyOnWriteArrayList<>();
        final AtomicLong sum = new AtomicLong(0);
        final CountDownLatch latch = new CountDownLatch(threadNum);

        final DirOnly dirOnly_0 = new DirOnly(latch, dirOnlyList, new AtomicInteger(1), sum);
        dirOnly_0.offer4Init(f);
        dirOnlyList.add(dirOnly_0);
        if (1 == threadNum) {
            executor.submit(dirOnly_0);
        } else {
            for (int i = 0; i < threadNum - 1; i++) {
                final DirOnly dirOnly = new DirOnly(latch, dirOnlyList, new AtomicInteger(0), sum);
                dirOnlyList.add(dirOnly);
                executor.submit(dirOnly);
            }
            executor.submit(dirOnly_0);
        }
        for (; ; ) {
            boolean l = false;
            try {
                l = latch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (l) {
                break;
            }
            log.info(dir + ":sum=" + sum.get());
        }
        return sum.get();
    }

    public static void main(String args[]) {
        String dir = "/";
        MyCount myCount = new MyCount(32);
        long sum = myCount.sum(dir);
        System.out.println(dir + ":sum=" + sum);
    }


}
