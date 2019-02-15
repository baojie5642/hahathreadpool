package com.baojie.zk.example.watcher.keepers;

import com.baojie.zk.example.watcher.refresh.ContextRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileWatcher implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);
    private final Path path;
    private final AtomicBoolean stop;
    private final WatchService service;
    private final ContextRefresher refresher;

    public static FileWatcher create(Path path, AtomicBoolean stop, ContextRefresher refresher) {
        if (null == path) {
            throw new IllegalStateException("path");
        } else {
            return new FileWatcher(path, stop, refresher);
        }
    }

    private FileWatcher(Path path, AtomicBoolean stop, ContextRefresher refresher) {
        this.path = path;
        this.stop = stop;
        this.refresher = refresher;
        this.service = getWatchService();
    }

    private WatchService getWatchService() {
        try {
            return FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log.error(e.toString() + ", path=" + path, e);
            throw new Error(e.getCause());
        } catch (Throwable t) {
            log.error(t.toString() + ", path=" + path, t);
            throw new Error(t.getCause());
        }
    }

    @Override
    public void run() {
        if (null == service) {
            log.error("WatchService null, path=" + path);
        } else {
            if (!register()) {
                closeService();
                log.error("file watcher register fail, path=" + path);
            } else {
                Throwable t0 = null;
                try {
                    watch();
                } catch (Throwable t) {
                    t0 = t;
                    log.error(t.toString() + ", path=" + path, t);
                } finally {
                    closeService();
                    log.info("hot load exit, path=" + path + ", error=" + t0);
                }
            }
        }
    }

    // 只注册一种类型的监控
    private boolean register() {
        // 针对当前文件的父文件夹进行监控
        // 不能对文件进行监控
        Path parent = path.getParent();
        try {
            parent.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
            return true;
        } catch (IOException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return false;
    }

    private void closeService() {
        if (null != service) {
            try {
                service.close();
            } catch (IOException e) {
                error(e);
            } catch (Throwable t) {
                error(t);
            }
        }
    }

    private void watch() {
        WatchKey key = null;
        final String tn = Thread.currentThread().getName();
        for (; ; ) {
            log.debug("config hot load working, tn=" + tn);
            key = getKey();
            if (null == key) {
                if (isStop()) {
                    return;
                } else {
                    continue;
                }
            }
            if (!key.isValid()) {
                log.error("WatchKey isValid==false, path=" + path);
                return;
            }
            flush();
            switchKey(key);
            if (!key.reset()) {
                log.error("key.reset==false, stop hot load, path=" + path);
                return;
            }
        }
    }

    private WatchKey getKey() {
        if (isStop()) {
            return null;
        }
        try {
            return service.poll(3600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            error(e);
        } catch (Throwable t) {
            error(t);
        }
        return null;
    }

    private void error(Throwable t) {
        if (null == t) {
            return;
        }
        if (stop.get()) {
            log.error(t.toString());
        } else {
            log.error(t.toString(), t);
        }
    }

    private boolean isStop() {
        return stop.get();
    }

    private void flush() {
        for (int i = 0; i < 8; i++) {
            Thread.yield();
        }
    }

    private void switchKey(WatchKey key) {
        List<WatchEvent<?>> enents = key.pollEvents();
        if (null == enents) {
            log.error("key.pollEvents null, path=" + path);
            return;
        }
        if (enents.isEmpty()) {
            return;
        }
        for (WatchEvent<?> event : enents) {
            if (null == event) {
                continue;
            }
            Kind<?> kind = event.kind();
            if (!getKind(kind)) {
                continue;
            }
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                reload();
                log.info("kind=" + kind + ", path=" + path);
            }
        }
    }

    private boolean getKind(Kind<?> kind) {
        if (null == kind) {
            log.error("kind null, path=" + path);
            return false;
        }
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return false;
        }
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return false;
        }
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return false;
        }
        return true;
    }

    private void reload() {
        ConfigurableEnvironment env = refresher.getContext().getEnvironment();
        Set<String> set = refresher.refreshEnvironment();
        checkAndNotify(set, env);
    }

    private void checkAndNotify(Set<String> set, ConfigurableEnvironment env) {
        if (null == set) {
            return;
        } else {
            for (String s : set) {
                String val = env.getProperty(s);
                log.info("key=" + s + ", has changed, new val=" + val);
                //notifyAsyncSched(s);
            }
        }
    }

    // 这里不做动态定时器重置
    // 由定时的runner自己执行一次结束时自己在finally里面判断
    private void notifyAsyncSched(String s) {
        if (s.contains("dura")) {

        }
    }

}
