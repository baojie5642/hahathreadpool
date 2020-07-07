package com.baojie.zk.example.concurrent.seda_pipe.future;

import java.util.concurrent.Future;

public interface StageFuture<V> extends Future<V> {

    boolean hasSubmit();

    Throwable cause();

}
