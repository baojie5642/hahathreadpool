package com.baojie.zk.example.concurrent.filecount;

enum Poison {

    P(-1),
    D(0);

    private final int value;

    Poison(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
        return "poison value=" + value;
    }

}
