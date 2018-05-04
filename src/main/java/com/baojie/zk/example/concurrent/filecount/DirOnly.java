package com.baojie.zk.example.concurrent.filecount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class DirOnly implements Runnable {
    //private static final Logger log = LoggerFactory.getLogger(DirOnly.class);
    private final ConcurrentLinkedQueue<File> files = new ConcurrentLinkedQueue<>();
    // 0,正常   1,自旋    2,park
    private final AtomicInteger step = new AtomicInteger(0);

    private final CountDownLatch latch;
    private final List<DirOnly> dirOnlyList;
    private final AtomicInteger myDirCount;
    private final AtomicLong sum;

    public DirOnly(CountDownLatch latch, List<DirOnly> dirOnlyList, AtomicInteger myDirCount, AtomicLong sum) {
        this.latch = latch;
        this.dirOnlyList = dirOnlyList;
        this.myDirCount = myDirCount;
        this.sum = sum;
    }

    public void offer4Init(File init) {
        if (null == init) {
            return;
        }
        files.offer(init);
    }

    @Override
    public void run() {
        final int listSize = dirOnlyList.size();
        File file = null;
        try {
            count:
            for (; ; ) {
                file = pollFile();
                if (null == file) {
                    stealOther();
                    for (int i = 0; i < listSize; i++) {
                        file = pollFile();
                        if (null != file) {
                            deal(file);
                        }
                    }
                    if (null == file) {
                        spin();
                        boolean exit = true;
                        for (int i = 0; i < listSize; i++) {
                            if (!isExitTime()) {
                                exit = false;
                            } else {
                                sleepMilliSec(1);
                            }
                        }
                        if (exit) {
                            break count;
                        } else {
                            lockPark();
                            step.set(0);
                        }
                    } else {
                        continue;
                    }
                } else {
                    deal(file);
                }
            }
        } finally {
            latch.countDown();
        }
    }

    private boolean isExitTime() {
        boolean exit = true;
        for (DirOnly dirOnly : dirOnlyList) {
            if (!dirOnly.isCountZero() || !dirOnly.isEmpty()) {
                exit = false;
                break;
            }
        }
        return exit;
    }

    private void spin() {
        step.set(1);
        for (int i = 0; i < 64; i++) {
            File file = pollFile();
            if (null != file) {
                deal(file);
            }
        }
    }

    private void lockPark() {
        step.set(2);
        sleepMilliSec(100);
    }

    private void stealOther() {
        File file = null;
        all:
        for (DirOnly dirOnly : dirOnlyList) {
            file = dirOnly.stealWork();
            if (null != file) {
                deal(file);
                one:
                for (; ; ) {
                    file = dirOnly.stealWork();
                    if (null == file) {
                        break one;
                    } else {
                        deal(file);
                        continue one;
                    }
                }
            } else {
                continue all;
            }
        }
    }

    private void deal(File file) {
        if (null == file) {
            return;
        } else {
            if (isFile(file)) {
                try {
                    addSum();
                } finally {
                    decCount();
                }
            } else if (isDir(file)) {
                File fs[] = splitFiles(file);
                if (null == fs || fs.length == 0) {
                    decCount();
                } else {
                    try {
                        for (File f : fs) {
                            if (null == f) {
                                continue;
                            } else {
                                if (isFile(f)) {
                                    addSum();
                                } else if (isDir(f)) {
                                    files.offer(f);
                                    addCount();
                                } else {
                                    addSum();
                                }
                            }
                        }
                    } finally {
                        decCount();
                    }
                }
            } else {
                try {
                    addSum();
                } finally {
                    decCount();
                }
            }
        }
    }

    private File[] splitFiles(File file) {
        if (null == file) {
            return null;
        } else {
            return file.listFiles();
        }
    }

    private boolean isFile(File file) {
        if (null == file) {
            return false;
        } else {
            return file.isFile();
        }
    }

    private boolean isDir(File file) {
        if (null == file) {
            return false;
        } else {
            return file.isDirectory();
        }
    }

    private File pollFile() {
        return files.poll();
    }

    private void sleepMilliSec(int milliSec) {
        LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(milliSec, TimeUnit.MILLISECONDS));
    }

    private void addCount() {
        add:
        for (; ; ) {
            int add = myDirCount.get();
            if (myDirCount.compareAndSet(add, add + 1)) {
                break add;
            }
        }
    }

    private void decCount() {
        dec:
        for (; ; ) {
            int dec = myDirCount.get();
            if (myDirCount.compareAndSet(dec, dec - 1)) {
                break dec;
            }
        }
    }

    private void addSum() {
        sum:
        for (; ; ) {
            long dec = sum.get();
            if (sum.compareAndSet(dec, dec + 1)) {
                break sum;
            }
        }
    }

    public boolean isEmpty() {
        return null == files.peek();
    }

    public boolean isCountZero() {
        return 0 == myDirCount.get();
    }

    public File stealWork() {
        File file = pollFile();
        if (null == file) {
            return null;
        } else {
            try {
                return file;
            } finally {
                decCount();
            }
        }
    }

}
