package com.baojie.zk.example.hotload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HotDog implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(HotDog.class);
    private final Path path;
    private final AtomicBoolean stop;
    private final WatchService service;
    private final List<Resource> resources;
    private final LinkedBlockingQueue<Resource> loadQueue;

    public static HotDog create(Path path, List<Resource> rs, AtomicBoolean stop, LinkedBlockingQueue<Resource> lq) {
        if (null == path) {
            throw new IllegalStateException("path");
        }
        if (null == rs) {
            throw new IllegalStateException("resources");
        }
        return new HotDog(path, rs, stop, lq);
    }

    private HotDog(Path path, List<Resource> resources, AtomicBoolean stop, LinkedBlockingQueue<Resource> lq) {
        this.path = path;
        this.stop = stop;
        this.loadQueue = lq;
        this.resources = resources;
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
            return;
        }
        boolean r = register();
        if (r == false) {
            closeService();
            log.error("file watcher register fail, path=" + path);
            return;
        }
        Throwable t0 = null;
        try {
            hotWatcher();
        } catch (Throwable t) {
            t0 = t;
            log.error("hot load exit, err=" + t.toString() + ", path=" + path, t);
        } finally {
            closeService();
            log.info("hot load exit, path=" + path + ", throwable=" + t0);
        }
    }

    // 只注册一种类型的监控
    private boolean register() {
        try {
            path.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
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
                printErr(e);
            } catch (Throwable t) {
                printErr(t);
            }
        }
    }

    private void hotWatcher() {
        WatchKey key = null;
        final String tn = Thread.currentThread().getName();
        for (; ; ) {
            if (stop.get()) {
                log.info("hot load dog exit, path=" + path);
                return;
            }
            log.debug("hotDog runner is working, threadName=" + tn);
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
            waitFlush();
            dealRightKey(key);
            waitFlush();
            if (!key.reset()) {
                closeService();
                log.error("key.reset==false, stop hot load, path=" + path);
                return;
            }
        }
    }

    private WatchKey getKey() {
        try {
            return service.poll(60, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            printErr(e);
        } catch (Throwable t) {
            printErr(t);
        }
        return null;
    }

    private void printErr(final Throwable t) {
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

    private void waitFlush() {
        Thread.yield();
    }

    private void dealRightKey(final WatchKey key) {
        List<WatchEvent<?>> listKey = key.pollEvents();
        if (null == listKey) {
            log.error("key.pollEvents null, path=" + path);
            return;
        }
        if (listKey.isEmpty()) {
            return;
        }
        for (WatchEvent<?> watchEvent : listKey) {
            Kind<?> kind = watchEvent.kind();
            if (!getKind(kind)) {
                continue;
            }
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                log.info("kind=" + kind + ", path=" + path);
                reload(watchEvent);
            }
        }
    }

    private boolean getKind(final Kind<?> kind) {
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

    private void reload(final WatchEvent<?> watchEvent) {
        final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
        final Path p = getPath(watchEventPath);
        if (null == p) {
            log.error("file io path null, WatchEvent=" + watchEventPath);
            sendAllResource();
            return;
        }
        URI uri = p.toUri();
        File f = shift2File(uri);
        if (null == f) {
            log.error("uri maybe enable, uri=" + uri + ", get from watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        String fn = f.getName();
        log.debug("fileName=" + fn + ", watchEvent path=" + p + ", spring path=" + path);
        if (fn.contains(".goutputstream")) {
            // 在ubuntu和centos虚拟机中名称是异常的，并且，在这些系统中需要点击两次保存才能使变量的替换生效
            // 但是在服务器上是可以的，都比较正常,这个是平台原因导致的，或者是虚拟机的原因，毕竟是java的nioPath
            log.warn("maybe Ubuntu or VisualCentOS, watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        if (!fn.contains(".properties")) {
            sendAllResource();
            return;
        }
        sendSpecialResource(fn);
    }

    private Path getPath(WatchEvent<Path> watchEventPath) {
        try {
            return watchEventPath.context();
        } catch (Throwable t) {
            printErr(t);
        }
        return null;
    }

    private File shift2File(URI uri) {
        try {
            return new File(uri);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private void sendAllResource() {
        if (null == resources) {
            return;
        }
        for (Resource r : resources) {
            if (isStop()) {
                log.debug("hot load has stop, to offer resource=" + r);
                return;
            }
            if (null != r) {
                log.debug("resource file name=" + r.getFilename());
                offerResource(r);
            }
        }
    }

    private void sendSpecialResource(String fn) {
        if (null == fn) {
            return;
        }
        if (null == resources) {
            return;
        }
        for (Resource r : resources) {
            if (isStop()) {
                log.debug("hot load has stop, to offer resource=" + r);
                return;
            }
            if (null == r) {
                continue;
            }
            String n = r.getFilename();
            if (null == n) {
                continue;
            }
            if (!n.equals(fn)) {
                continue;
            }
            offerResource(r);
            log.debug("get special resource=" + r + ", fileName=" + n);
            break;
        }
    }

    private void offerResource(Resource r) {
        if (!loadQueue.offer(r)) {
            log.error("offer resource fail, resource=" + r);
        }
    }

}
