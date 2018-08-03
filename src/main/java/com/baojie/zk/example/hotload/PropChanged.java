package com.baojie.zk.example.hotload;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropChanged implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PropChanged.class);
    private final SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    private final LinkedBlockingQueue<Resource> resourceQueue;
    private final ResourceHandler rh;
    private final AtomicBoolean stop;
    private final Semaphore signal;

    public PropChanged(LinkedBlockingQueue<Resource> rq, AtomicBoolean stop, Semaphore signal, ResourceHandler rh) {
        this.resourceQueue = rq;
        this.signal = signal;
        this.stop = stop;
        this.rh = rh;
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
        Resource r;
        final String tn = Thread.currentThread().getName();
        for (; ; ) {
            if (stop.get()) {
                return;
            }
            log.debug("propChanged runner is working, threadName=" + tn);
            r = getResource();
            if (null == r) {
                continue;
            } else {
                reload(r);
            }
        }
    }

    private Resource getResource() {
        try {
            return resourceQueue.poll(60, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            printErr(e);
        } catch (Throwable t) {
            printErr(t);
        }
        return null;
    }

    private void printErr(final Throwable t) {
        if (stop.get()) {
            log.error(t.toString());
        } else {
            log.error(t.toString(), t);
        }
    }

    private void reload(Resource r) {
        Properties p = shift2Prop(r);
        if (null == p) {
            log.error("spring load properties null, Resource=" + r);
            return;
        }
        final Properties sp = rh.getProperties();
        if (null == sp) {
            log.error("get spring properties null, resource=" + r);
            return;
        }
        // 以新的properties文件遍历为准
        Iterator it = p.stringPropertyNames().iterator();
        while (it.hasNext()) {
            String spPropKey = (String) it.next();
            String oldValue = sp.getProperty(spPropKey);
            String newValue = p.getProperty(spPropKey);
            if (null == oldValue || null == newValue) {
                log.warn("oldValue=" + oldValue + ", newValue" + newValue + ", spPropKey=" + spPropKey);
            }
            if (null == oldValue && null == newValue) {
                log.warn("newValue and oldValue null, spPropKey=" + spPropKey);
                continue;
            }
            // 如果原来的值为oldString=old,当修改properties文件中的值为空时，也就是newString=blank
            // 这时，null == newValue是成立的，所以这时的修改会失效
            if (StringUtils.isBlank(newValue)) {
                log.error("null == newValue, oldValue=" + oldValue + ", spPropKey=" + spPropKey + ", resource=" + r);
                continue;
            }
            if (!sp.containsKey(spPropKey)) {
                log.error("no contains key, old=" + oldValue + ", ey=" + spPropKey + ", r=" + r + ", new=" + newValue);
                continue;
            }
            if (!newValue.equals(oldValue)) {
                changeProperties(spPropKey, p, r, sp);
                log.debug("value has changed, newValue=" + newValue + ", oldValue=" + oldValue + ", resource=" + r);
            }
        }
    }

    private Properties shift2Prop(Resource r) {
        try {
            return PropertiesLoaderUtils.loadProperties(r);
        } catch (Throwable t) {
            log.error(t.toString() + ", resource=" + r, t);
        }
        return null;
    }

    private void changeProperties(String spPropKey, Properties p, Resource r, Properties springInnerProp) {
        final Map<String, BeanPropHolder> beanMap = rh.getProcessor().getBeanProp();
        if (null == beanMap) {
            log.error("hotLoadProcessor.getBeanProp null");
            return;
        }
        if (null == spPropKey) {
            return;
        }
        BeanPropHolder holder = beanMap.get(spPropKey);
        if (null == holder) {
            log.error("get BeanPropHolder null, spPropKey=" + spPropKey);
            return;
        }
        Object newBeanValue = p.get(spPropKey);
        if (null == newBeanValue) {
            log.error("newBeanValue null, spPropKey=" + spPropKey);
            return;
        }
        Object newRealValue = convert(newBeanValue, holder.getField());
        if (null == newRealValue) {
            log.error("convert fail, BeanPropHolder=" + holder + ", springPropKey=" + spPropKey);
            return;
        }
        if (doChange(holder, newRealValue)) {
            updateSpringProp(spPropKey, springInnerProp, newRealValue);
        } else {
            log.error("change properties fail, springPropKey=" + spPropKey + ", holder=" + holder + ", resource" + r);
        }
    }

    private Object convert(Object newBeanValue, Field fieldToUpdate) {
        try {
            return typeConverter.convertIfNecessary(newBeanValue, fieldToUpdate.getType());
        } catch (TypeMismatchException matchExe) {
            log.error(matchExe.toString(), matchExe);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

    private boolean doChange(BeanPropHolder holder, Object newRealValue) {
        Object beanToUpdate = holder.getBean();
        Field fieldToUpdate = holder.getField();
        try {
            fieldToUpdate.set(beanToUpdate, newRealValue);
            return true;
        } catch (IllegalAccessException e) {
            log.error(e.toString(), e);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return false;
    }

    private void updateSpringProp(String spPropKey, Properties springInnerProp, Object real) {
        if (springInnerProp.containsKey(spPropKey)) {
            springInnerProp.replace(spPropKey, real);
            log.debug("has replace, key=" + spPropKey + ", new=" + real);
        } else {
            springInnerProp.put(spPropKey, real);
            log.debug("has put, key=" + spPropKey + ", new=" + real);
        }
        // 提醒配置文件的内存监控线程，打印当前的配置文件的值
        signal.release(1);
        // 检测一下是否需要重新构造一下异步的定时器
        restartAsyncWork(spPropKey);
    }

    private static final String MAIN_KEY = "dura";

    // 可以实现信号量的监控或者定时任务的重新启动或者初始化
    // 已经实现，不过本人上除了再上传的
    private void restartAsyncWork(String spPropKey) {
        if (null == spPropKey) {
            return;
        }
        if (!spPropKey.contains(MAIN_KEY)) {
            return;
        }
        log.debug("file monitor notify to rebuild, propKey=" + spPropKey);
    }

}
