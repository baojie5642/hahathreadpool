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
    private final CountDownLatch latch;
    private final AtomicInteger l;
    private final AtomicLong sum;

    public FileOnly(LinkedBlockingQueue<CoreFile> files, CountDownLatch latch, AtomicInteger l, AtomicLong sum) {
        this.files = files;
        this.latch = latch;
        this.l = l;
        this.sum = sum;
    }

    @Override
    public void run() {
        getFile:
        for (; ; ) {
            CoreFile cf = timePoll();
            if (null == cf) {
                int level = l.get();
                if (0 == level) {
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
                Poison poison = cf.getP();
                if (Poison.P == poison) {
                    int level = l.get();
                    if (0 == level) {
                        CoreFile peek = files.peek();
                        if (null == peek) {
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
                        add:
                        for (; ; ) {
                            long s = sum.get();
                            if (sum.compareAndSet(s, s + 1)) {
                                break add;
                            }
                        }
                    } else if (file.isDirectory()) {
                        //log.error("file is Dir, name=" + file);
                        continue getFile;
                    } else {
                        //log.error("unknow file, name=" + file);
                        continue getFile;
                    }
                }
            }
        }
    }

    private CoreFile timePoll() {
        for (int i = 0; i < 64; i++) {
            CoreFile cf = files.poll();
            if (null != cf) {
                return cf;
            }
        }
        try {
            return files.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //log.error(e.toString(), e);
        }
        return null;
    }


}
