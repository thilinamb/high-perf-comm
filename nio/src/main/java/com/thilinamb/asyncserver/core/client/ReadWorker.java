package com.thilinamb.asyncserver.core.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Author: Thilina
 * Date: 3/8/14
 */
public class ReadWorker extends Thread {

    private final ByteBuffer buffer;
    private final SocketChannel socketChannel;

    private Logger logger = LogManager.getLogger(ReadWorker.class);

    public ReadWorker(SocketChannel socketChannel) {
        buffer = ByteBuffer.allocate(20);   // SHA1 hash values are 160 bits
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        while (true){
            synchronized (this){
                try {
                    wait();
                    int count = socketChannel.read(buffer);
                    if(count == -1){
                        socketChannel.close();
                        break;
                    }

                    if(!buffer.hasRemaining()){
                        buffer.flip();
                        byte[] receivedData = new byte[20];
                        buffer.get(receivedData);

                        buffer.clear();
                    }
                } catch (InterruptedException e) {
                    // thread terminating
                } catch (IOException e) {
                    try {
                        logger.error(e.getMessage(), e);
                        socketChannel.close();
                    } catch (IOException ignore) {

                    }
                }

            }
        }
    }

    public synchronized void wakeUp(){
        this.notify();
    }
}
