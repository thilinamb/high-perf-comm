package com.thilinamb.asyncserver.core.client;

import java.util.Random;

/**
 * @author Thilina Buddhika
 */
public class PayloadGenerator extends Thread{

    private final WriteWorker writeWorker;

    private String[] messagePool = new String[]{"Hello!", "Hello World!", "Ayubowan!", "Arbitrary Message!"};
    private Random random = new Random();


    public PayloadGenerator(WriteWorker writeWorker) {
        super();
        this.writeWorker = writeWorker;
    }

    @Override
    public void run() {
        while (true){
            writeWorker.addPayload(messagePool[random.nextInt(messagePool.length)].getBytes());
        }
    }
}
