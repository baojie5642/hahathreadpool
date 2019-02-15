package com.baojie.zk.example.watcher.cloud;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

public class LoggingSystemShutdownListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    public static final int DEFAULT_ORDER = BootstrapApplicationListener.DEFAULT_ORDER + 1;

    private int order = DEFAULT_ORDER;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        shutdownLogging();
    }

    private void shutdownLogging() {
        LoggingSystem loggingSystem = LoggingSystem.get(ClassUtils.getDefaultClassLoader());
        loggingSystem.cleanUp();
        loggingSystem.beforeInitialize();
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

}

