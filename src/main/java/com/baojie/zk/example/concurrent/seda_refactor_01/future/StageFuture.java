package com.baojie.zk.example.concurrent.seda_refactor_01.future;

import java.util.concurrent.Future;

public interface StageFuture<V> extends Future<V> {

    boolean hasSubmit();

    Throwable cause();

}
