package com.baojie.zk.example.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnitedThreadGroup {
	private static final Logger log = LoggerFactory.getLogger(UnitedThreadGroup.class);

	private UnitedThreadGroup() {

	}

	public static final ThreadGroup getGroup() {
		ThreadGroup tg = null;
		SecurityManager sm = System.getSecurityManager();
		if (null != sm) {
			tg = sm.getThreadGroup();
		} else {
			log.warn("SecurityManager can be null when get ThreadGroup, ignore this");
			tg = Thread.currentThread().getThreadGroup();
		}
		if (null == tg) {
			log.error("ThreadGroup get from Main(JVM) must not be null");
			throw new NullPointerException("ThreadGroup get from Main(JVM) must not be null");
		}
		return tg;
	}

}
