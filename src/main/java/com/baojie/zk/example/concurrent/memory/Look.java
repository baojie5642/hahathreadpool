package com.baojie.zk.example.concurrent.memory;

import java.util.concurrent.TimeUnit;

public class Look {

    public static final void main(String args[]) {
        final Button button = new Button();
        for (int i = 0; i < 100; i++) {
            MemRun run = new MemRun(button);
            Thread t = new Thread(run);
            t.start();
        }
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // java语法，带有应用性质的值传递
        // 感觉比volatile好用
        button.setOpen(false);
    }

}
