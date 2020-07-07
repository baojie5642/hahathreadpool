package com.baojie.zk.example.concurrent.seda_pipe.task;

import com.baojie.zk.example.concurrent.seda_pipe.bus.Bus;

public interface Call<V> {

    V call(Bus bus) throws Exception;

}
