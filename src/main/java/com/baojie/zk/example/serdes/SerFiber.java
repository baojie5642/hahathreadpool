package com.baojie.zk.example.serdes;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerFiber<T, V> extends Fiber<V> {

    private static final Logger log = LoggerFactory.getLogger(SerFiber.class);

    private final T obj;
    private final Channel<byte[]> channel;

    public SerFiber(T obj, Channel<byte[]> channel) {
        super();
        this.obj = obj;
        this.channel = channel;
    }

    @Override
    public V run() {
        Class<T> cls;
        Schema<T> sch;
        if (null == obj) {
            sendBytes(new byte[0]);
        } else if (null == (cls = (Class<T>) obj.getClass())) {
            sendBytes(new byte[0]);
        } else if (null == (sch = ObjectUtil.schema(cls))) {
            sendBytes(new byte[0]);
        } else {
            sendBytes(work(sch));
        }
        return null;
    }

    private byte[] work(Schema<T> schema) {
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            release(buffer);
        }
    }

    private void release(LinkedBuffer buffer) {
        if (null != buffer) {
            buffer.clear();
            buffer = null;
        }
    }

    private void sendBytes(byte[] bs) {
        try {
            channel.send(bs);
        } catch (SuspendExecution se) {
            log.error(se.toString(), se);
        } catch (InterruptedException ie) {
            log.error(ie.toString(), ie);
        } finally {
            close();
        }
    }

    private void close() {
        channel.close();
    }

}
