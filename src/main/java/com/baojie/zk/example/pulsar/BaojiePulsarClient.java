package com.baojie.zk.example.pulsar;

import org.apache.pulsar.client.api.*;

import java.util.concurrent.TimeUnit;

public class BaojiePulsarClient {

    public static void main(String args[]) throws Exception {
        PulsarClient client = PulsarClient.builder()
                .serviceUrl("pulsar://localhost:6650")
                .build();
        Producer<String> producer = client.newProducer(Schema.STRING)
                .topic("my-pulsar-topic")
                .create();
        //producer.closeAsync()
        //        .thenRun(() -> System.out.println("Producer closed"));
        final Consumer consumer = client.newConsumer()
                .topic("my-pulsar-topic")
                .subscriptionName("my-pulsar-sub")
                .subscribe();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    Message msg = null;
                    // Wait for a message
                    try {
                        msg = consumer.receive();
                    } catch (PulsarClientException e) {
                        e.printStackTrace();
                    }
                    if (null != msg) {
                        System.out.println("consumer Message received:" + new String(msg.getData()));
                    }
                    // Acknowledge the message so that it can be deleted by the message broker
                    try {
                        consumer.acknowledge(msg);
                    } catch (PulsarClientException e) {
                        e.printStackTrace();
                    }
                } while (true);
            }
        }, "consumer");
        t.start();
        Thread.yield();
        TimeUnit.SECONDS.sleep(3);
        producer.send("baojie pulsar sync message");
        for (; ; ) {
            TimeUnit.SECONDS.sleep(3000);
            producer.sendAsync("baojie pulsar async message").thenAccept(msgId -> {
                System.out.println("producer Message with ID %s successfully sent" + msgId);
            });
        }
        //producer.close();
        //client.close();
    }

}
