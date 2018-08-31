package com.baojie.zk.example.serdes;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesFiber<T, V> extends Fiber<V> {

    private static final Logger log = LoggerFactory.getLogger(DesFiber.class);

    private final Class<T> cls;
    private final byte[] bs;
    private final Channel<T> channel;

    public DesFiber(Class<T> cls, byte[] bs,  Channel<T> channel) {
        super();
        this.bs = bs;
        this.cls = cls;
        this.channel = channel;
    }

    @Override
    public V run() {
        T type = ObjectUtil.getType(cls);
        Schema<T> sch;
        if (null == type) {
            log.error("get type null,cls=" + cls);
            close();
        } else if(null==(sch=ObjectUtil.schema(cls))){
            log.error("get schema null,cls=" + cls);
            close();
        }else {
            boolean s = false;
            try {
                s = deserialize(type,sch);
            } finally {
                send(type, s);
            }
        }
        return null;
    }

    private boolean deserialize(T type,Schema<T> sch) {
        boolean suc = false;
        try {
            ProtostuffIOUtil.mergeFrom(bs, 0, bs.length, type, sch);
            suc = true;
        } catch (Throwable te) {
            log.error(te.toString() + ",class=" + cls, te);
        } finally {
            return suc;
        }
    }

    private void send(T t, boolean suc) {
        try {
            if (suc) channel.send(t);
        } catch (SuspendExecution se) {
            log.error(se.toString() + ",class=" + cls, se);
        } catch (InterruptedException ie) {
            log.error(ie.toString() + ",class=" + cls, ie);
        } finally {
            close();
        }
    }

    private void close() {
        channel.close();
    }

}
