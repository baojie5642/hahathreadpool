package com.baojie.zk.example.hotload_springboot;

import com.baojie.zk.example.hotload_springboot.watch.Change;
import com.baojie.zk.example.hotload_springboot.watch.WatchDog;
import com.baojie.zk.example.ip.IPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ComponentScan(basePackages = {"com.baojie"})
public class Config  {

    private static final String RESOURCE_PATH = "classpath:props/";
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final Map<String, Val> cache;
    private final CopyOnWriteArrayList<File> resource = new CopyOnWriteArrayList<>();

    private final Semaphore sem = new Semaphore(1);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<>(1024);

    private final ConfigMonitor monitor = new ConfigMonitor("order_config_monitor", new CountDownLatch(1));

    // 暂时只监控一个配置文件的文件夹
    private final Path watch;

    // 这里使用springboot的特性，初始化完成之前已经加载好了
    // 因为application是作为boot的高优先级配置先行加载
    @HotReloadable("${api.server.port}")
    private static volatile int port;

    @Autowired
    public Config() {
        this.cache=BeanAdapter.getCache();
        File path = props();
        if (null == path) {
            throw new NullPointerException();
        }
        Path w = watch(path);
        if (null == w) {
            throw new NullPointerException();
        }
        this.watch = w;
        fill(files(path));
        initSem();
        monitor();
        this.monitor.keep();
        sem.release(1);
    }


    private void initSem() {
        try {
            sem.tryAcquire(1, 3, TimeUnit.SECONDS);
        } catch (Throwable te) {
            throw new Error(te.getCause());
        }
    }

    private void monitor() {
        WatchDog dog = WatchDog.create(watch, resource, stop, queue);
        Thread td = new Thread(dog, "config-watch-dog");
        td.start();
        Change change = new Change(queue, stop, sem, cache);
        Thread tc = new Thread(change, "config-prop-change");
        tc.start();
    }

    private File props() {
        File path = null;
        try {
            path = new File(ResourceUtils.getURL(RESOURCE_PATH).getPath());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getCause());
        } catch (Throwable te) {
            throw new Error(te.getCause());
        }
        return path;
    }

    private Path watch(File path) {
        URI uri = path.getAbsoluteFile().toURI();
        return Paths.get(uri);
    }

    // 参考了fileutil的一些实现，不适合这种简单的判断
    private List<File> files(File path) {
        if (!path.isDirectory()) {
            throw new IllegalArgumentException();
        }
        if (path.isFile()) {
            throw new IllegalArgumentException();
        }
        File[] fs = path.listFiles();
        if (null == fs || fs.length <= 0) {
            throw new IllegalArgumentException();
        }
        List<File> list = new ArrayList<>();
        for (File f : fs) {
            String n = f.getName();
            if (null == n) {
                continue;
            }
            if (!n.endsWith(".properties")) {
                continue;
            } else {
                list.add(f);
            }
        }
        if (list.size() <= 0) {
            throw new IllegalArgumentException();
        } else {
            return list;
        }
    }

    private void fill(List<File> list) {
        try {
            for (File f : list) {
                if (null != f) {
                    resource.addIfAbsent(f);
                }
            }
        } finally {
            list.clear();
        }
    }

    @PreDestroy
    public void destory() {
        try {
            stop.set(true);
            int size = resource.size();
            outer:
            for (int i = 0; i < size; i++) {
                File f = resource.get(i);
                if (null != f) {
                    inner:
                    for (; ; ) {
                        if (queue.offer(f)) {
                            break inner;
                        }
                    }
                    break outer;
                }
            }
            sem.release(1);
        } finally {
            resource.clear();
        }
    }

    private void print() {
        log.debug("/*****************************  start print config info  *******************************/");
        log.debug("api server port=" + getApiPort());
        log.debug("/********  api monitor  ********/");
        log.debug("get IP=" + IPConfig.ip());
        log.debug("ipFaceName=" + IPConfig.ipFaceName());
        log.debug("/*****************************  finish print config info  *******************************/");
    }

    public static final int getApiPort() {
        int val = port;
        if (val <= 0) {
            return 8083;
        } else {
            return val;
        }
    }


    private final class ConfigMonitor extends KeepVolatile4Config {

        public ConfigMonitor(String name, CountDownLatch latch) {
            super(name, latch);
        }

        @Override
        public void work() {
            try {
                for (; ; ) {
                    if (stop.get()) {
                        return;
                    } else {
                        acquire();
                    }
                    if (stop.get()) {
                        return;
                    } else {
                        print();
                    }
                }
            } finally {
                log.debug(name + " has stopped");
            }
        }

        private void acquire() {
            try {
                sem.acquire(1);
            } catch (InterruptedException e) {
                logErr(e);
            } catch (Throwable t) {
                logErr(t);
            }
        }

        private void logErr(Throwable t) {
            if (!stop.get()) {
                log.error(t.toString(), t);
                // 如果没有显示的中断，那么擦除中断状态
                Thread.currentThread().interrupted();
            }
        }

    }

    private abstract class KeepVolatile4Config implements Runnable {

        protected final Logger log = LoggerFactory.getLogger(KeepVolatile4Config.class);
        private final CountDownLatch latch;
        protected final Thread thread;
        protected final String name;

        protected KeepVolatile4Config(String name, CountDownLatch latch) {
            if (null == name || name.length() == 0) {
                throw new IllegalStateException();
            }
            this.name = name;
            this.latch = latch;
            this.thread = new Thread(this, name);
        }

        public void keep() {
            try {
                thread.start();
            } finally {
                latch.countDown();
            }
        }

        public void work() {
            throw new IllegalArgumentException();
        }

        @Override
        public void run() {
            try {
                waitLatch();
            } finally {
                work();
            }
        }

        private void waitLatch() {
            boolean suc = false;
            try {
                suc = latch.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error(e.toString(), e);
            } catch (Throwable t) {
                log.error(t.toString(), t);
            }
            if (suc) {
                return;
            } else {
                throw new Error();
            }
        }
    }


}
