package com.baojie.zk.example.hotload;

import com.baojie.zk.example.concurrent.FutureCancel;
import com.baojie.zk.example.concurrent.PoolShutDown;
import com.baojie.zk.example.concurrent.TFactory;
import com.baojie.zk.example.concurrent.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropertiesManager {
    private static final Logger log = LoggerFactory.getLogger(PropertiesManager.class);
    private final LinkedBlockingQueue<Resource> rq = new LinkedBlockingQueue<>(8192);
    private final Map<Path, List<Resource>> m4r = new ConcurrentHashMap<>(32);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final List<Future<?>> fs = new ArrayList<>(32);
    private final ResourceHandler rh;
    private final ThreadPool pool;

    public PropertiesManager(ResourceHandler rh) {
        if (null == rh) {
            throw new IllegalArgumentException();
        }
        this.rh = rh;
        dealResource();
        this.pool = createPool();
        log.info("propertiesManager start completed");
    }

    public void startHotLoad(Semaphore signal) {
        HotDog dog;
        PropChanged propChanged;
        for (Map.Entry<Path, List<Resource>> e : m4r.entrySet()) {
            dog = HotDog.create(e.getKey(), e.getValue(), stop, rq);
            propChanged = new PropChanged(rq, stop, signal, rh);
            fs.add(pool.submit(dog));
            fs.add(pool.submit(propChanged));
        }
        log.info("propertiesManager runners working");
    }

    public void destory() {
        if (stop.get()) {
            return;
        } else {
            if (stop.compareAndSet(false, true)) {
                shutDownPool();
                cleanMap();
                log.info("propertiesManager service shutdown completed");
            } else {
                return;
            }
        }
    }

    private void shutDownPool() {
        try {
            FutureCancel.cancel(fs, true);
        } finally {
            PoolShutDown.threadPool(pool, "properties_manager");
        }
    }

    private void dealResource() {
        List<Resource> list = shift();
        URI uri;
        Path path;
        for (Resource r : list) {
            File f = getFile(r);
            if (null == f) {
                continue;
            }
            uri = f.getParentFile().toURI();
            path = getPathFromURI(uri);
            if (null == path) {
                continue;
            }
            if (m4r.containsKey(path)) {
                m4r.get(path).add(r);
            } else {
                List<Resource> ls = new ArrayList<>();
                ls.add(r);
                m4r.putIfAbsent(path, ls);
            }
        }
    }

    private List<Resource> shift() {
        Resource[] resources = rh.getLocations();
        if (null == resources) {
            throw new IllegalStateException("Resource[]");
        }
        final int l = resources.length;
        if (0 >= l) {
            return new ArrayList<>(0);
        }
        List<Resource> list = new ArrayList<>(l);
        for (int i = 0; i < l; i++) {
            Resource r = resources[i];
            if (null != r) {
                list.add(r);
            }
        }
        return list;
    }

    private File getFile(final Resource r) {
        if (null == r) {
            return null;
        }
        try {
            return r.getFile();
        } catch (IOException e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    private Path getPathFromURI(URI uri) {
        if (null == uri) {
            return null;
        }
        return Paths.get(uri);
    }

    private ThreadPool createPool() {
        int size = m4r.size() * 2;
        // 使用交换器队列，因为runner是一个for循环loop监控，但是没有添加拒绝策略
        return new ThreadPool(size, size * 2, 180, TimeUnit.SECONDS, new SynchronousQueue<>(),
                TFactory.create("hotLoad_properties"));
    }

    private void cleanMap() {
        try {
            for (List<Resource> r : m4r.values()) {
                if (null != r) {
                    r.clear();
                }
            }
        } finally {
            m4r.clear();
        }
    }

}
