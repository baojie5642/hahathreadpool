package com.baojie.zk.example.concurrent.filecount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DirOnly implements Runnable {
    //private static final Logger log = LoggerFactory.getLogger(DirOnly.class);
    private final LinkedBlockingQueue<CoreFile> files;
    private final LinkedBlockingQueue<CoreFile> dirs;
    private final AtomicInteger l;
    private final CountDownLatch latch;

    public DirOnly(LinkedBlockingQueue<CoreFile> files, LinkedBlockingQueue<CoreFile> dirs, AtomicInteger l,
            CountDownLatch latch) {
        this.files = files;
        this.dirs = dirs;
        this.l = l;
        this.latch = latch;
    }

    @Override
    public void run() {
        int level = l.get();
        if (0 == level) {
            //dirs.offer(CoreFile.create(Poison.P,null));
            //files.offer(CoreFile.create(Poison.P,null));
            latch.countDown();
        } else {
            CoreFile cf;
            getDir:
            for (; ; ) {
                cf = timePoll();
                if (null == cf) {
                    int le = l.get();
                    if (0 == le) {
                        if (null == dirs.peek()) {
                            latch.countDown();
                            break getDir;
                        } else {
                            continue getDir;
                        }
                    }
                } else {
                    Poison poison = cf.getP();
                    if (Poison.P == poison) {
                        int le = l.get();
                        if (0 == le) {
                            if (null == dirs.peek()) {
                                latch.countDown();
                                break getDir;
                            } else {
                                continue getDir;
                            }
                        }
                    } else {
                        File file = cf.getF();
                        boolean noDir = true;
                        if (!file.isDirectory()) {
                            throw new IllegalArgumentException();
                        } else {
                            File fs[] = file.listFiles();
                            for (File f : fs) {
                                if (f.isDirectory()) {
                                    noDir = false;
                                    dirs.offer(CoreFile.create(Poison.D, f));
                                } else if (f.isFile()) {
                                    files.offer(CoreFile.create(Poison.D, f));
                                } else {
                                    //log.error("file name="+f+", file not Dir not File");
                                }
                            }

                            if (noDir) {
                                int ll ;
                                down:
                                for (; ; ) {
                                    ll = l.get();
                                    if (l.compareAndSet(ll, ll - 1)) {
                                        break down;
                                    }
                                }
                                ll = l.get();
                                if (ll == 0) {
                                    if (null == dirs.peek()) {
                                        latch.countDown();
                                        break getDir;
                                    } else {
                                        continue getDir;
                                    }
                                }
                            } else {
                                int ll ;
                                up:
                                for (; ; ) {
                                    ll = l.get();
                                    if (l.compareAndSet(ll, ll + 1)) {
                                        break up;
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    private CoreFile timePoll() {
        for (int i = 0; i < 64; i++) {
            CoreFile cf = dirs.poll();
            if (null != cf) {
                return cf;
            }
        }
        try {
            return dirs.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //log.error(e.toString(),e);
        }
        return null;
    }


}
