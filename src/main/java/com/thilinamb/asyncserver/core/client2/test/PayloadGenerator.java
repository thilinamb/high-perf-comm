package com.thilinamb.asyncserver.core.client2.test;

import com.thilinamb.asyncserver.core.client2.ChannelWriterDataHolder;

import java.util.Random;

/**
 * @author Thilina Buddhika
 */
public class PayloadGenerator implements Runnable {

    private final ChannelWriterDataHolder dataHolder;
    private Random random = new Random();


    public PayloadGenerator(ChannelWriterDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public byte[] getRandomPayload(){
        byte[] bytes = new byte[200];
        random.nextBytes(bytes);
        return bytes;
    }

    @Override
    public void run() {
        System.out.println("Initliazing sending load.");
        while (true) {
            dataHolder.writeData(getRandomPayload());
        }
    }
}
