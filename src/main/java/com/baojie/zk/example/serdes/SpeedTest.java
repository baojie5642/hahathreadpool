package com.baojie.zk.example.serdes;


import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SpeedTest {

    private final long id;
    private final String name;
    private final Map<String, String> info;

    public SpeedTest(String name,long id,Map<String,String> info){
        this.id=id;
        this.info=info;
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public Map<String, String> getInfo() {
        return info;
    }




    public static void main(String[] args){
        SpeedTest test=null;
        Map<String,String> map=null;
        AtomicLong sum=new AtomicLong(0);
        long fm= TimeUnit.NANOSECONDS.convert(5,TimeUnit.MINUTES);
        long start=System.nanoTime();
        long i=0;
        for(;;){
            map=new HashMap<>(4);
            map.put("a","a");
            map.put("b","b");
            map.put("c","c");
            map.put("d","d");
            test=new SpeedTest(i+"",i++,map);

            byte[] bs=SerializeUtil.serialize(test);
            if(bs==null){
                throw new NullPointerException();
            }

            SpeedTest get=SerializeUtil.deserialize(bs,SpeedTest.class);

            if(get==null){
                throw new NullPointerException();
            }
            System.out.println(get+"__"+i);
        }
    }

}
