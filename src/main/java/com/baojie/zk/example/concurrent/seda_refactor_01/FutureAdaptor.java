package com.baojie.zk.example.concurrent.seda_refactor_01;

import java.util.concurrent.Future;

public interface FutureAdaptor<V> extends Future<V> {

    boolean hasSubmit();

    Throwable cause();

}
