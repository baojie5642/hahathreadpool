package com.baojie.zk.example.serdes;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;

public class SerializeUtil {

    public static final <T> byte[] serialize(final T obj) {
        Class<T> cls=(Class<T>) obj.getClass();
        Schema<T> sch=ObjectUtil.schema(cls);
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(obj, sch, buffer);
        } finally {
            if (null != buffer) {
                buffer.clear();
            }
        }
    }

    public static final <T> T deserialize(final byte[] bs, final Class<T> cls) {
        T type = ObjectUtil.getType(cls);
        Schema<T> sch=ObjectUtil.schema(cls);
        try {
            ProtostuffIOUtil.mergeFrom(bs, 0, bs.length, type, sch);
        } catch (Throwable te) {

        } finally {

        }
        return type;
    }

}
