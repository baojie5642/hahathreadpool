package com.baojie.zk.example.concurrent.seda_refactor_01;

import com.baojie.zk.example.sleep.Sleep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String args[]) {
        final class TestObj {
            private String name;

            public TestObj() {

            }

            public void setName(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

        }

        final class Ha implements Runnable {
            private final Logger log = LoggerFactory.getLogger(Ha.class);
            private final TestObj obj;

            public Ha(TestObj obj) {
                this.obj = obj;
            }

            @Override
            public void run() {
                String tn = Thread.currentThread().getName();
                for (; ; ) {
                    log.info("my name is=" + tn + ", test obj name=" + obj.getName());
                    //Sleep.sleep(3, TimeUnit.SECONDS);
                    Thread.yield();
                }
            }
        }

        final class Heng implements Runnable {
            private final Logger log = LoggerFactory.getLogger(Heng.class);
            private final TestObj obj;

            public Heng(TestObj obj) {
                this.obj = obj;
            }

            @Override
            public void run() {
                String tn = Thread.currentThread().getName();
                int i = 0;
                for (; ; ) {
                    Sleep.locksptSleep(TimeUnit.SECONDS, 1);
                    obj.setName(i + "");
                    log.info("has set name=" + obj.getName());
                    Thread.yield();
                    i++;
                }
            }
        }

        final TestObj obj = new TestObj();
        final Heng heng = new Heng(obj);

        Thread ts = new Thread(heng, "I am set");
        ts.start();

        final Ha ha = new Ha(obj);

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(ha, "I am read");
            t.start();
        }

    }
}
