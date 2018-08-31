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

            Channel<byte[]> ch0= Channels.newChannel(4);
            SerFiber sf=new SerFiber(test,ch0);
            sf.start();
            byte[] bs=null;
            try {
                bs=ch0.receive();
            } catch (SuspendExecution suspendExecution) {
                suspendExecution.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(bs==null){
                throw new NullPointerException();
            }
            ch0.close();
            Channel<SpeedTest> ch1= Channels.newChannel(4);
            SpeedTest get=null;
            DesFiber df=new DesFiber(SpeedTest.class,bs,ch1);
            df.start();
            try {
                get=ch1.receive();
            } catch (SuspendExecution suspendExecution) {
                suspendExecution.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(get==null){
                throw new NullPointerException();
            }
            ch1.close();
            System.out.println(i);
        }
    }

}
