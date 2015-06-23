package com.thilinamb.asyncserver.core.client;

import java.util.Random;

/**
 * @author Thilina Buddhika
 */
public class PayloadGenerator extends Thread{

    private final WriteWorker writeWorker;

    private Random random;

    public PayloadGenerator(WriteWorker writeWorker) {
        super();
        this.writeWorker = writeWorker;
        random = new Random();
    }

    public byte[] getRandomPayload(){
        byte[] bytes = new byte[1024*100];
        random.nextBytes(bytes);
        return bytes;
    }

    @Override
    public void run() {
        while (true){
            writeWorker.addPayload(getRandomPayload());
        }
    }
}
