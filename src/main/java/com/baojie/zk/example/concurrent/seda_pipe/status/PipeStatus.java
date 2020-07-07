package com.baojie.zk.example.concurrent.seda_pipe.status;

public enum PipeStatus {

    FIXED("FIXED"),
    DYNAMIC("DYNAMIC"),
    NORMAL("NORMAL"),
    CACHED("CACHED");

    private final String status;
    PipeStatus(final String status) {
        this.status = status;
    }

    public String value(){
        return status;
    }

}
