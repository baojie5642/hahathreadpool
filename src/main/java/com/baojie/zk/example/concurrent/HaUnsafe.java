package com.baojie.zk.example.concurrent;

import jdk.internal.misc.Unsafe;

public class HaUnsafe {

	private HaUnsafe() {

	}

	public static Unsafe getUnsafe() {
		return Unsafe.getUnsafe();
	}

}
