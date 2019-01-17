package com.baojie.zk.example.concurrent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MemRun implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MemRun.class);
    private final Button button;

    public MemRun(Button button) {
        this.button = button;
    }

    @Override
    public void run() {
        String tn = Thread.currentThread().getName();
        while (button.isOpen()) {
            log.info("i am '" + tn + "', running");
            try {
                TimeUnit.MICROSECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("i am '" + tn + "', stopped");
    }

}
