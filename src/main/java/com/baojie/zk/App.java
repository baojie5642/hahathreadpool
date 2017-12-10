package com.baojie.zk;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Hello world!
 */
public class App {
    private final LinkedBlockingQueue<Object> oq = new LinkedBlockingQueue<>(32);

    public App() {

    }

    public void add() {
        oq.offer(new Object());
    }

    public List<Object> drainQueue() {
        BlockingQueue<Object> q = oq;
        ArrayList<Object> taskList = new ArrayList<Object>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Object r : q.toArray(new Object[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    public int size() {
        return oq.size();
    }

    public static void main(String[] args) {
        App app = new App();
        for (int i = 0; i < 30; i++) {
            app.add();
        }
        List<Object> list = app.drainQueue();
        System.out.println(list.size());
        System.out.println(app.size());
    }
}
