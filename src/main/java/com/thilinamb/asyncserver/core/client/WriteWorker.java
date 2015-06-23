package com.thilinamb.asyncserver.core.client;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: Thilina
 * Date: 3/8/14
 */
public class WriteWorker extends Thread {

    private final SocketChannel socketChannel;
    private ByteBuffer byteBuffer;

    private final Logger logger = LogManager.getLogger(WriteWorker.class);

    private Lock lock;
    private Condition condition;

    public WriteWorker( SocketChannel channel, int byteBufferSizeInMb) {
        this.socketChannel = channel;
        byteBuffer = ByteBuffer.allocate(1024 * 1024 * byteBufferSizeInMb);
        lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    wait();
                    lock.lock();
                        if(byteBuffer.remaining() < byteBuffer.capacity()){
                            byteBuffer.flip();
                            socketChannel.write(byteBuffer);
                            if(!byteBuffer.hasRemaining()){
                                byteBuffer.clear();
                            } else {
                                byteBuffer.compact();
                            }
                            condition.signalAll();
                        } else {
                            condition.await();
                        }
                        /*if (byteBuffer.position() == 0) {
                            final byte[] payload = messagePool[random.nextInt(4)].getBytes();
                            byteBuffer.putInt(payload.length);
                            messageSize = payload.length;
                            byteBuffer.put(payload);
                            byteBuffer.flip();
                        }
                        socketChannel.write(byteBuffer);
                        if (!byteBuffer.hasRemaining()) {
                            byteBuffer.clear();
                            if(logger.isDebugEnabled()){
                                logger.debug("Sent a message to " + socketChannel.getRemoteAddress());
                            }
                        }*/
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
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public synchronized void wakeUp(){
        this.notify();
    }

    public void addPayload(byte[] payload){
        try {
            lock.lock();
            while (byteBuffer.remaining() < (payload.length + 4)){
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            byteBuffer.putInt(payload.length).put(payload);
            StatCollector.getInstance().updateStatistics(payload.length);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
