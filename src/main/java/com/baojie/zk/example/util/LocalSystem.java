package com.baojie.zk.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

public class LocalSystem {
    private static final Logger log = LoggerMaker.logger();

    public static final String JAVA_SPECIFICATION_VERSION = getSystemProperty("java.specification.version");

    private static final int MAX_BLOCK_SIZE = 32 * 1024 * 1024; // 32 MB

    private static final int MIN_BLOCK_SIZE = 4 * 1024 * 1024;  // 4 MB

    private static final int PAGE_SIZE;

    private static final int N_CPU;

    static {
        try {
            PAGE_SIZE = Unsafe.getUnsafe().pageSize();//LocalUnsafe.getUnsafe().pageSize();
            N_CPU = Runtime.getRuntime().availableProcessors();
        } catch (Throwable t) {
            log.error(t.toString(), t);
            throw new Error(t.getCause());
        }
    }

    public static final int cpus() {
        return N_CPU;
    }

    public static final int pageSize() {
        return PAGE_SIZE;
    }

    private static final String getSystemProperty(final String property) {
        try {
            return System.getProperty(property);
        } catch (final SecurityException ex) {
            return null;
        }
    }

    private static final JavaVersion JAVA_SPECIFICATION_VERSION_AS_ENUM = JavaVersion.get(JAVA_SPECIFICATION_VERSION);

    public static final boolean isJavaVersionAtLeast(JavaVersion requiredVersion) {
        return JAVA_SPECIFICATION_VERSION_AS_ENUM.atLeast(requiredVersion);
    }

    public static final int blockSize() {
        int size = cpus() * pageSize();
        if (size < MIN_BLOCK_SIZE) {
            return MAX_BLOCK_SIZE;
        } else {
            return size;
        }
    }

    private LocalSystem() {
        throw new IllegalArgumentException();
    }

}
