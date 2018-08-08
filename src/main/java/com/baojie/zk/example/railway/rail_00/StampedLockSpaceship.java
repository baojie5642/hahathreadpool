package com.baojie.zk.example.railway.rail_00;

import java.util.concurrent.locks.StampedLock;

public class StampedLockSpaceship implements Spaceship {
    private final StampedLock lock = new StampedLock();

    private int x;
    private int y;

    public int readPosition(final int[] coordinates) {
        int tries = 1;
        long stamp = lock.tryOptimisticRead();

        coordinates[0] = x;
        coordinates[1] = y;

        if (!lock.validate(stamp)) {
            ++tries;
            stamp = lock.readLock();
            try {
                coordinates[0] = x;
                coordinates[1] = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return tries;
    }

    public int move(final int xDelta, final int yDelta) {
        final long stamp = lock.writeLock();
        try {
            x += xDelta;
            y += yDelta;
        } finally {
            lock.unlockWrite(stamp);
        }

        return 1;
    }
}
