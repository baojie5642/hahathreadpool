package com.baojie.zk.example.concurrent.filecount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileOnly implements Runnable {

    //private static final Logger log = LoggerFactory.getLogger(FileOnly.class);
    private final LinkedBlockingQueue<CoreFile> files;
    private final LinkedBlockingQueue<CoreFile> dirs;
    private final CountDownLatch latch;
    private final AtomicInteger l;
    private final AtomicLong sum;
    private final int fileThreadNum;

    public FileOnly(LinkedBlockingQueue<CoreFile> dirs, LinkedBlockingQueue<CoreFile> files, CountDownLatch latch,
            AtomicInteger l, AtomicLong sum, int fileThreadNum) {
        this.dirs = dirs;
        this.files = files;
        this.latch = latch;
        this.l = l;
        this.sum = sum;
        this.fileThreadNum = fileThreadNum;
    }

    @Override
    public void run() {
        getFile:
        for (; ; ) {
            CoreFile cf = timePoll();
            if (null == cf) {
                int level = l.get();
                if ((0 == level && latch.getCount() <= fileThreadNum) || (0 == level && dirs.isEmpty())) {
                    if (null == files.peek()) {
                        latch.countDown();
                        break getFile;
                    } else {
                        continue getFile;
                    }
                } else {
                    continue getFile;
                }
            } else {
                File file = cf.getF();
                if (null == file) {
                    //log.error("get file null in FileOnly");
                    continue getFile;
                }
                if (file.isFile()) {
                    upFileSum();
                    continue getFile;
                } else if (file.isDirectory()) {
                    //log.error("file is Dir in FileOnly, name=" + file);
                    continue getFile;
                } else {
                    //log.error("unknow file, name=" + file);
                    upFileSum();
                    continue getFile;
                }
            }
        }
    }

    private CoreFile timePoll() {
        CoreFile cf = null;
        for (int i = 0; i < 64; i++) {
            cf = files.poll();
            if (null != cf) {
                return cf;
            }
        }
        if (null == cf) {
            if (l.get() == 0) {
                return null;
            }
        }
        try {
            return files.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //log.error(e.toString(), e);
        }
        return null;
    }

    private void upFileSum() {
        up:
        for (; ; ) {
            long upSum = sum.get();
            if (sum.compareAndSet(upSum, upSum + 1)) {
                break up;
            }
        }
    }
}
