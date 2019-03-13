package com.baojie.zk.example.concurrent.seda_refactor_01.task;

import com.baojie.zk.example.concurrent.seda_refactor_01.bus.Bus;

public interface Call<V> {

    V call(Bus bus) throws Exception;

}
