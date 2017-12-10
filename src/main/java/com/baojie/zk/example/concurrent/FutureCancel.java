package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

public final class FutureCancel {
    private static final Logger log = LoggerFactory.getLogger(FutureCancel.class);

    private FutureCancel() {

    }

    public static void cancel(final Future<?> future) {
        if (null == future) {
            log.debug("future null");
            return;
        }
        future.cancel(true);
    }

    public static void cancelScheduled(final ScheduledFuture<?> scheduledFuture) {
        if (null == scheduledFuture) {
            log.debug("scheduledFuture null");
            return;
        }
        scheduledFuture.cancel(true);
    }

    public static void cancel(final List<Future<?>> futures, final boolean clean) {
        if (null == futures) {
            log.debug("list futures null");
            return;
        }
        for (Future<?> future : futures) {
            if (null != future) {
                future.cancel(true);
            }
        }
        if (clean) {
            futures.clear();
        }
    }

    public static void cancelScheduled(final List<ScheduledFuture<?>> scheduledFutures, final boolean clean) {
        if (null == scheduledFutures) {
            log.debug("list scheduledFutures null");
            return;
        }
        for (ScheduledFuture<?> future : scheduledFutures) {
            if (null != future) {
                future.cancel(true);
            }
        }
        if (clean) {
            scheduledFutures.clear();
        }
    }

}
