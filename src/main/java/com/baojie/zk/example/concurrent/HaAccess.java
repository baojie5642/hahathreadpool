package com.baojie.zk.example.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class HaAccess {

	private final Sync sync;

	public static HaAccess create() {
		return create(false);
	}

	public static HaAccess create(final boolean setAccess) {
		return new HaAccess(setAccess);
	}

	private HaAccess() {
		this(false);
	}

	private HaAccess(final boolean mutex) {
		sync = new Sync();
		set(mutex);
	}

	public void get() throws InterruptedException {
		sync.innerGet();
	}

	public void get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		sync.innerGet(unit.toNanos(timeout));
	}

	public void set(final boolean mutex) {
		if (mutex) {
			sync.innerSetTrue();
		} else {
			sync.innerSetFalse();
		}
	}

	public boolean state() {
		return sync.innerState();
	}

	private static final class Sync extends AbstractQueuedSynchronizer {

		private static final long serialVersionUID = 2016122714592955555L;
		private static final int TRUE = 1;
		private static final int FALSE = 2;

		private boolean isTrue(final int state) {
			final int innerState = (state & TRUE);
			boolean innerTrue = false;
			if (innerState != 0) {
				innerTrue = true;
			} else {
				innerTrue = false;
			}
			return innerTrue;
		}

		protected int tryAcquireShared(final int state) {
			final int innerState = getState();
			final boolean innerTrue = isTrue(innerState);
			int returnFlag = 0;
			if (innerTrue) {
				returnFlag = 1;
			} else {
				returnFlag = -1;
			}
			return returnFlag;
		}

		protected boolean tryReleaseShared(final int ignore) {
			return true;
		}

		public boolean innerState() {
			final int getInnerState = getState();
			final boolean innerTrue=isTrue(getInnerState);
			return innerTrue;
		}

		public void innerGet() throws InterruptedException {
			acquireSharedInterruptibly(0);
		}

		public void innerGet(final long nanosTimeout) throws InterruptedException, TimeoutException {
			final boolean innerAcquire=tryAcquireSharedNanos(0, nanosTimeout);
			if (!innerAcquire){
				throw new TimeoutException();
			}else {
				return;
			}	
		}

		public void innerSetTrue() {
			int s=-7;
			for (;;) {
				s = getState();
				if (s == TRUE) {
					return;
				}
				if (compareAndSetState(s, TRUE)) {
					releaseShared(0);
					return;
				}
			}
		}

		public void innerSetFalse() {
			int s=-7;
			for (;;) {
				s = getState();
				if (s == FALSE) {
					return;
				}
				if (compareAndSetState(s, FALSE)) {
					return;
				}
			}
		}
	}

}
