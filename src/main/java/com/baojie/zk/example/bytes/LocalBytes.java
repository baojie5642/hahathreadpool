package com.baojie.zk.example.bytes;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public final class LocalBytes {

    private static final Logger log = LoggerFactory.getLogger(LocalBytes.class);

    private static final ConcurrentHashMap<Class<?>, Schema<?>> cache = new ConcurrentHashMap<>(8192);

    private static final Objenesis objenesis = new ObjenesisStd(true);

    private static final byte[] EMPTY = new byte[0];

    static {
        System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
    }

    private LocalBytes() {
        throw new IllegalArgumentException();
    }

    public static final <T> byte[] seria(final T obj) {
        if (null == obj) {
            return EMPTY;
        } else {
            return real(obj);
        }
    }

    private static final <T> byte[] real(final T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        Schema<T> schema = schema(cls);
        LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
            return work(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    private static final <T> Schema<T> schema(final Class<T> cls) {
        Schema<?> schema = cache.get(cls);
        if (null == schema) {
            Schema<?> newer = RuntimeSchema.getSchema(cls);
            schema = cache.putIfAbsent(cls, newer);
            if (schema == null) {
                schema = newer;
            }
        }
        return (Schema<T>) schema;
    }

    private static final <T> byte[] work(final T obj, final Schema<T> schema, final LinkedBuffer buffer) {
        byte[] bytes = null;
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        } finally {
            if ((null == bytes) || (bytes.length == 0)) {
                bytes = EMPTY;
            }
        }
        return bytes;
    }

    public static final <T> T deseria(final byte[] bytes, final Class<T> cls) {
        if (null == bytes || null == cls) {
            throw new NullPointerException();
        }
        if (bytes.length <= 0) {
            throw new IllegalArgumentException();
        }
        T tmp = instance(cls);
        if (null == tmp) {
            throw new IllegalStateException();
        }
        Schema<T> schema = schema(cls);
        try {
            ProtostuffIOUtil.mergeFrom(bytes, 0, bytes.length, tmp, schema);
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return tmp;
    }

    private static final <T> T instance(final Class<T> cls) {
        ObjectInstantiator<T> tiator = objenesis.getInstantiatorOf(cls);
        if (null == tiator) {
            T t = objenesis.newInstance(cls);
            return instance(t, cls);
        } else {
            T t = tiator.newInstance();
            return instance(t, cls);
        }
    }

    private static final <T> T instance(final T t, final Class<T> cls) {
        if (null != t) {
            return t;
        } else {
            return localJvm(cls);
        }
    }

    private static final <T> T localJvm(final Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            log.error(t.toString(), t);
        }
        return null;
    }

}
