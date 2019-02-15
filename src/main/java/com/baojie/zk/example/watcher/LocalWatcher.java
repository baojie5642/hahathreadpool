package com.baojie.zk.example.watcher;

import com.baojie.zk.example.watcher.cloud.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;

public class LocalWatcher extends Watcher {

    public LocalWatcher() {
        super();
    }

    @Override
    public void watch(ConfigurableApplicationContext context, RefreshScope refresh) {
        super.watch(context, refresh);
    }

    @Override
    public void stop() {
        super.stop();
    }

}
