package com.baojie.zk.example.concurrent.seda_refactor_01;

public class SimuStage {

    public static final class Baojie implements Bus {
        public Baojie() {

        }

    }

    public static void main(String args[]) {
        Baojie bj = new Baojie();
        Stage<Baojie> stage = new Stage<>(1, 1, 180, "baojie-stage", bj);
        Baojie bus = stage.getBus();
        if (bus instanceof Bus) {
            System.out.println("ok");
        }
        stage.shutdown();
    }

}
