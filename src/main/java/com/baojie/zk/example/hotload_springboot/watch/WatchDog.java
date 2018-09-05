package com.baojie.zk.example.hotload_springboot.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchDog implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WatchDog.class);
    private final Path path;
    private final AtomicBoolean stop;
    private final WatchService service;
    private final List<File> resources;
    private final LinkedBlockingQueue<File> queue;

    public static WatchDog create(Path path, List<File> rs, AtomicBoolean stop, LinkedBlockingQueue<File> lq) {
        if (null == path) {
            throw new IllegalStateException("path");
        }
        if (null == rs) {
            throw new IllegalStateException("resources");
        }
        return new WatchDog(path, rs, stop, lq);
    }

    private WatchDog(Path path, List<File> resources, AtomicBoolean stop, LinkedBlockingQueue<File> lq) {
        this.path = path;
        this.stop = stop;
        this.queue = lq;
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
        if (!register()) {
            closeService();
            log.error("file watcher register fail, path=" + path);
            return;
        }
        Throwable t0 = null;
        try {
            hotWatcher();
        } catch (Throwable t) {
            t0 = t;
            log.error( t.toString() + ", path=" + path, t);
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
        try{
            if (null != service) {
                try {
                    service.close();
                } catch (IOException e) {
                    print(e);
                } catch (Throwable t) {
                    print(t);
                }
            }
        }finally {

        }
    }

    private void hotWatcher() {
        WatchKey key = null;
        final String tn = Thread.currentThread().getName();
        for (; ; ) {
            log.debug("hotDog runner is working, tn=" + tn);
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
            dealRightKey(key);
            flush();
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
            return service.poll(1800, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            print(e);
        } catch (Throwable t) {
            print(t);
        }
        return null;
    }

    private void print(Throwable t) {
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

    private void dealRightKey(WatchKey key) {
        List<WatchEvent<?>> listKey = key.pollEvents();
        if (null == listKey) {
            log.error("key.pollEvents null, path=" + path);
            return;
        }
        if (listKey.isEmpty()) {
            return;
        }
        for (WatchEvent<?> we : listKey) {
            if (null == we) {
                continue;
            }
            Kind<?> kind = we.kind();
            if (!getKind(kind)) {
                continue;
            }
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                log.info("kind=" + kind + ", path=" + path);
                reload(we);
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

    private void reload(WatchEvent<?> watchEvent) {
        WatchEvent<Path> we = (WatchEvent<Path>) watchEvent;
        Path p = getPath(we);
        if (null == p) {
            log.error("file io path null, watchEvent=" + we + ", path=" + path);
            sendAllResource();
            return;
        }
        URI uri = p.toUri();
        File f = shift2File(uri);
        if (null == f) {
            log.error("shift file null, uri=" + uri + ", get watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        String fn = f.getName();
        log.debug("fileName=" + fn + ", watchEvent path=" + p + ", spring path=" + path);
        if (fn.contains(".goutputstream")) {
            // 在ubuntu和centos虚拟机中名称是异常的，并且，在这些系统中需要点击两次保存才能使变量的替换生效
            // 但是在服务器上是可以的，都比较正常,这个是平台原因导致的，或者是虚拟机的原因，毕竟是java的nioPath
            // 网上查询资料，这是ubuntu的小bug，貌似还未解决，有待查证
            log.warn("maybe Ubuntu or VisualCentOS, watchEvent path=" + p + ", spring path=" + path);
            sendAllResource();
            return;
        }
        if (!fn.contains(".properties")) {
            sendAllResource();
        } else {
            sendSpecialResource(fn);
        }
    }

    private Path getPath(WatchEvent<Path> we) {
        try {
            return we.context();
        } catch (Throwable t) {
            print(t);
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
        for (File f : resources) {
            if (isStop()) {
                log.debug("hot load has stop, to offer file=" + f);
            }
            if (null != f) {
                log.debug("file name=" + f.getName());
                offerResource(f);
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
        for (File f : resources) {
            if (isStop()) {
                log.debug("hot load has stop, to offer file=" + f);
                return;
            }
            if (null == f) {
                continue;
            }
            String n = f.getName();
            if (null == n) {
                continue;
            }
            if (!n.equals(fn)) {
                continue;
            } else {
                offerResource(f);
                log.debug("get special f=" + f + ", fileName=" + n);
                break;
            }
        }
    }

    private void offerResource(File f) {
        if (!queue.offer(f)) {
            log.error("offer file fail, file=" + f);
        }
    }

}
