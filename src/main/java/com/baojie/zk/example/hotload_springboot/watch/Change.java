package com.baojie.zk.example.hotload_springboot.watch;

import com.baojie.zk.example.hotload_springboot.Val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Change implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Change.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final LinkedBlockingQueue<File> queue;
    private final AtomicBoolean stop;
    private final Semaphore signal;
    private final Map<String, Val> cache;

    public Change(LinkedBlockingQueue<File> queue, AtomicBoolean stop, Semaphore signal, Map<String, Val> cache) {
        this.queue = queue;
        this.signal = signal;
        this.stop = stop;
        this.cache = cache;
    }

    @Override
    public void run() {
        try {
            work();
        } finally {
            log.info("propChanged runner exit");
        }
    }

    private void work() {
        File f;
        final String tn = Thread.currentThread().getName();
        for (; ; ) {
            if (stop.get()) {
                return;
            }
            log.debug("hotload runner is working, tn=" + tn);
            f = getResource(tn);
            if (stop.get()) {
                return;
            }
            if (null == f) {
                continue;
            } else {
                reload(f);
            }
        }
    }

    private File getResource(String tn) {
        if (stop.get()) {
            return null;
        }
        try {
            return queue.poll(180, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            error(e, tn);
        } catch (Throwable t) {
            error(t, tn);
        }
        return null;
    }

    private void error(Throwable t, String tn) {
        if (stop.get()) {
            log.error(t.toString() + ", thread name=" + tn);
        } else {
            log.error(t.toString() + ", thread name=" + tn, t);
        }
    }

    private void reload(File r) {
        Properties p = props(r);
        if (null == p) {
            log.error("spring load properties null, resource=" + r);
            return;
        }
        // 以新的properties文件遍历为准
        Iterator<String> it = p.stringPropertyNames().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = p.getProperty(key);
            if (null != value) {
                change(key, value);
            }
        }
    }

    private Properties props(File f) {
        if (f.isDirectory()) {
            return null;
        }
        if (!f.isFile()) {
            return null;
        }
        Properties p = new Properties();
        try (InputStream in = openInputStream(f)) {
            p.load(in);
            return p;
        } catch (Throwable te) {

        }
        return null;
    }

    private FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    private void change(String key, String value) {
        Val v = cache.get(key);
        if (null == v) {
            return;
        } else {
            Field field = v.getField();
            Object bean = v.getBean();
            Object fv = fieldValue(field, bean);
            if (null == fv) {
                convert(value, field, bean);
            } else {
                String fvs = fv.toString();
                if (!value.equals(fvs)) {
                    convert(value, field, bean);
                }
            }
        }
        // 唤醒config类的监听
        signal.release(1);
        // 后面还可以执行其他监听
    }

    private Object fieldValue(Field field, Object bean) {
        ReflectionUtils.makeAccessible(field);
        try {
            return field.get(bean);
        } catch (Throwable te) {

        }
        return null;
    }

    private boolean convert(Object val, Field field, Object bean) {
        Object rv = typeConverter.convertIfNecessary(val, field.getType());
        try {
            field.set(bean, rv);
            return true;
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return false;
    }
}
