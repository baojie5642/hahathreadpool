package com.baojie.zk.example.concurrent.seda_refactor_01;

public interface StageCall<V> {

    V call(Bus bus) throws Exception;

}
