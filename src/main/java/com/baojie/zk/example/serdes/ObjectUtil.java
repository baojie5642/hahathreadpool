package com.baojie.zk.example.serdes;

import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public final class ObjectUtil {

    private static final Logger log = LoggerFactory.getLogger(ObjectUtil.class);

    private static final ConcurrentHashMap<Class<?>, Schema<?>> cache = new ConcurrentHashMap<>();
    private static final Objenesis objenesis = new ObjenesisStd(true);

    static {
        System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
    }

    private ObjectUtil() {
        throw new IllegalArgumentException();
    }

    public static final <T> Schema<T> schema(Class<T> cls) {
        if (null == cls) {
            return null;
        }
        Schema<T> sch = (Schema<T>) cache.get(cls);
        if (null == sch) {
            sch = RuntimeSchema.getSchema(cls);
            if (null != sch) {
                cache.putIfAbsent(cls, sch);
                return sch;
            } else {
                return null;
            }
        } else {
            return sch;
        }
    }

    public static final <T> T getType(Class<T> cls) {
        if (null == cls) {
            return null;
        }
        T t = objenesis.newInstance(cls);
        if (null != t) {
            return t;
        } else {
            return ObjectUtil.self(cls);
        }
    }

    private static final <T> T self(Class<T> cls) {
        T t = null;
        try {
            t = cls.newInstance();
        } catch (Throwable te) {
            log.error(te.toString() + ",class=" + cls, te);
        }
        return t;
    }

}
