package com.baojie.zk.example.concurrent.filecount;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MyCount {

    private final ThreadPoolExecutor executor;

    private final int dirNum;

    private final int fileNum;

    public MyCount(int dirNum, int fileNum) {
        this.dirNum = dirNum;
        this.fileNum = fileNum;
        this.executor = new ThreadPoolExecutor(dirNum + fileNum, 2 * (dirNum + fileNum), 180, TimeUnit.SECONDS, new
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
        final LinkedBlockingQueue<CoreFile> dirs = new LinkedBlockingQueue();
        final LinkedBlockingQueue<CoreFile> files = new LinkedBlockingQueue<>();
        final AtomicInteger level = new AtomicInteger(1);
        final AtomicLong sum = new AtomicLong(0);
        final CoreFile cf = CoreFile.create(Poison.D, f);
        dirs.offer(cf);
        final CountDownLatch latch = new CountDownLatch(fileNum + dirNum);
        for (int i = 0; i < fileNum; i++) {
            FileOnly fileOnly = new FileOnly(files, latch, level, sum);
            executor.submit(fileOnly);
        }
        for (int j = 0; j < dirNum; j++) {
            DirOnly dirOnly = new DirOnly(files, dirs, level, latch);
            executor.submit(dirOnly);
        }
        try {
            latch.await(3600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sum.get();
    }

    public static void main(String args[]) {
        String dir = "/";

        MyCount myCount = new MyCount(32, 128);

        long sum = myCount.sum(dir);

        System.out.println(dir + ":sum=" + sum);
    }


}
