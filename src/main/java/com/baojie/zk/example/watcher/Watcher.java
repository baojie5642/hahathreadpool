package com.baojie.zk.example.watcher;

import com.baojie.zk.example.watcher.cloud.RefreshScope;
import com.baojie.zk.example.watcher.keepers.FileWatcher;
import com.baojie.zk.example.watcher.refresh.ContextRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Watcher {
    private static final Logger log = LoggerFactory.getLogger(LocalWatcher.class);
    private static final char PART_0 = '[';
    private static final char PART_1 = ']';
    private static final String KEY_0 = "application.yml";
    private static final String KEY_1 = "application.yaml";
    private static final String KEY_2 = "application.properties";
    // Note the order is from least to most specific (last one wins)
    private static final String DEFAULT_SEARCH_LOCATIONS = "classpath:/,classpath:/config/,file:./,file:./config/";
    private final CopyOnWriteArrayList<Thread> threads = new CopyOnWriteArrayList<>();
    private final AtomicBoolean stop = new AtomicBoolean(false);

    protected Watcher() {

    }

    protected void stop() {
        if (stop.get()) {
            return;
        } else {
            if (stop.compareAndSet(false, true)) {
                doStop();
            }
        }
    }

    private void doStop() {
        for (Thread t : threads) {
            if (null != t) {
                t.interrupt();
            }
        }
        threads.clear();
    }

    protected void watch(ConfigurableApplicationContext context, RefreshScope refresh) {
        ConfigurableEnvironment env = context.getEnvironment();
        ContextRefresher refresher = new ContextRefresher(context, refresh);
        MutablePropertySources target = env.getPropertySources();
        DefaultResourceLoader loader = new DefaultResourceLoader();
        for (PropertySource<?> source : target) {
            String name = source.getName();
            if (contains(name)) {
                doWatch(name, loader, refresher);
            }
        }
    }

    private boolean contains(String name) {
        if (name.contains(KEY_0)) {
            return true;
        } else if (name.contains(KEY_1)) {
            return true;
        } else if (name.contains(KEY_2)) {
            return true;
        } else {
            return false;
        }
    }

    private void doWatch(String name, ResourceLoader loader, ContextRefresher refresher) {
        String localFilePath = localFilePath(name);
        Resource res = loader.getResource(localFilePath);
        if (hasText(res)) {
            register(localFilePath, res, refresher);
        }
    }

    private String localFilePath(String name) {
        int len = name.length();
        boolean meet = false;
        StringBuilder sbu = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (c == '[') {
                meet = true;
            } else {
                if (meet) {
                    if (c == ']') {
                        break;
                    } else {
                        sbu.append(c);
                    }
                }
            }
        }
        String path = sbu.toString();
        log.debug("spring source name=" + name + ", path=" + path);
        return path;
    }

    private boolean hasText(Resource res) {
        if (null == res || !res.exists()) {
            return false;
        } else {
            String name = StringUtils.getFilenameExtension(res.getFilename());
            if (StringUtils.hasText(name)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private void register(String localFilePath, Resource res, ContextRefresher refresher) {
        URI uri = uri(res);
        if (null != uri) {
            Path path = path(uri);
            if (null != path) {
                doRegister(path, localFilePath, refresher);
            }
        }
    }

    private URI uri(Resource res) {
        try {
            //return res.getURI();
            // 修复bug,不能像上面那样获取uri,在某些linux环境下会出现,地址不透明的情况
            return res.getFile().getParentFile().toURI();
        } catch (IOException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private Path path(URI uri) {
        try {
            return Paths.get(uri);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private void doRegister(Path p, String localFilePath, ContextRefresher refresher) {
        FileWatcher watcher = FileWatcher.create(p, stop, refresher);
        String wn = localFilePath + "-watcher";
        Thread t = new Thread(watcher, wn);
        threads.addIfAbsent(t);
        t.start();
        log.debug("file watcher=" + wn + ", has registed, full path=" + p.toFile());
    }

}
