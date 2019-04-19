package com.baojie.zk.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class LoggerMaker {

    public static final Logger logger() {
        return LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    }

    private LoggerMaker() {
        throw new IllegalArgumentException();
    }

}
