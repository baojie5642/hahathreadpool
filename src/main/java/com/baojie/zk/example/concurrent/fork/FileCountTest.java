package com.baojie.zk.example.concurrent.fork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FileCountTest {


    public static final Logger log = LoggerFactory.getLogger(FileCountTest.class);

    private final static ForkJoin forkJoinPool = new ForkJoin();

    private static class FileSizeFinder extends RecursiveAction {

        private final File dir;
        private final AtomicLong count;

        public FileSizeFinder(final File dir, final AtomicLong count) {
            this.dir = dir;
            this.count = count;
        }

        public final long count() {
            return count.get();
        }

        @Override
        public void compute() {
            if (null == dir) {
                return;
            } else {
                if (isDirectory(dir)) {
                    final File[] children = listFiles(dir);
                    if (null == children) {
                        return;
                    } else {
                        int size = children.length;
                        if (size <= 0) {
                            return;
                        } else {
                            final LinkedList<FileSizeFinder> tasks = new LinkedList<>();
                            for (int i = 0; i < size; i++) {
                                File child = children[i];
                                if (isDirectory(child)) {
                                    final FileSizeFinder finder = new FileSizeFinder(child, count);
                                    tasks.add(finder);
                                } else {
                                    count.incrementAndGet();
                                }
                                children[i] = null;
                            }
                            final Collection<FileSizeFinder> result = invokeAll(tasks);
                            //await(result);
                        }
                    }
                } else {
                    count.incrementAndGet();
                }
            }
        }

        private final void await(Collection<FileSizeFinder> tasks) {
            for (final FileSizeFinder task : invokeAll(tasks)) {
                task.join();
            }
        }

        private final boolean isDirectory(final File file) {
            return file.isDirectory();
        }

        private final File[] listFiles(final File dir) {
            return dir.listFiles();
        }

    }

    public static void main(final String[] args) {

        final AtomicLong count = new AtomicLong(0);
        final FileSizeFinder finder = new FileSizeFinder(new File("/"),count);

        final CountDownLatch latch = new CountDownLatch(1);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    boolean suc = false;
                    try {
                        suc = latch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        suc = false;
                    } catch (Throwable e) {
                        suc = false;
                    }
                    if (suc) {
                        System.out.println("total file num=" + count.get());
                        break;
                    } else {
                        System.out.println("my file size=" + count.get());
                    }
                }
            }
        });
        thread.start();
        final long start = System.nanoTime();
        forkJoinPool.invoke(finder);
        final long end = System.nanoTime();
        latch.countDown();
        System.out.println("count file num=" + finder.count());
        long dura = end - start;
        System.out.println("start time=" + start + ", end time=" + end + ", dura=" + dura);
        System.out.println("Time taken: " + (end - start) / 1.0e9);
        System.out.println("m=" + TimeUnit.NANOSECONDS.toMillis(dura) + ", s=" + TimeUnit.NANOSECONDS.toSeconds(dura));
    }


}
