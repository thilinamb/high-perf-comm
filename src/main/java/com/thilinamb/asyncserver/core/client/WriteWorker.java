package com.thilinamb.asyncserver.core.client;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Thilina
 * Date: 3/8/14
 */
public class WriteWorker extends Thread {

    private final int sleepInterval;
    private final SocketChannel socketChannel;
    private ByteBuffer byteBuffer;

    private AtomicLong counter = new AtomicLong();
    private AtomicLong data = new AtomicLong();

    private final Logger logger = LogManager.getLogger(WriteWorker.class);

    private String[] messagePool = new String[]{"Hello!", "Hello World!", "Ayubowan!", "Arbitrary Message!"};
    private Random random = new Random();

    private long lastTimeStamp = 0;

    public WriteWorker(int sleepInterval, SocketChannel channel) {
        this.sleepInterval = sleepInterval;
        this.socketChannel = channel;
        byteBuffer = ByteBuffer.allocate(1024 * 8);
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    wait();

                    if(lastTimeStamp == 0){
                        lastTimeStamp = System.currentTimeMillis();
                    }

                    if (byteBuffer.position() == 0) {
                        final byte[] payload = messagePool[random.nextInt(4)].getBytes();
                        byteBuffer.putInt(payload.length);
                        data.getAndAdd(payload.length);
                        byteBuffer.put(payload);
                        byteBuffer.flip();
                    }
                    socketChannel.write(byteBuffer);
                    if (!byteBuffer.hasRemaining()) {
                        byteBuffer.clear();
                        if(logger.isDebugEnabled()){
                            logger.debug("Sent a message to " + socketChannel.getRemoteAddress());
                        }
                        counter.getAndIncrement();
                        if(counter.get() % 100000 == 0){
                            long timeNow = System.currentTimeMillis();
                            logger.info("Sent " + 100000 + " messages in " + (timeNow - lastTimeStamp) + "ms. " +
                                    "Throughput: " + ((double)100000*1000)/(timeNow - lastTimeStamp) +
                                    ", Data Rate(KB/s):" + ((data.get()*1000)/((1024)*(timeNow - lastTimeStamp))));
                            data.set(0);
                            lastTimeStamp = timeNow;
                        }
                    }
                    // ensure the message rate
                    //Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    // terminating
                } catch (IOException e) {
                    try {
                        logger.error(e.getMessage(), e);
                        socketChannel.close();
                    } catch (IOException ignore) {
                        // ignore
                    }
                }
            }
        }
    }

    public synchronized void wakeUp(){
        this.notify();
    }
}
