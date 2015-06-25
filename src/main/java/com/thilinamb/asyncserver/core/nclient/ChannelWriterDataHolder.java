package com.thilinamb.asyncserver.core.nclient;

import com.thilinamb.asyncserver.core.client.StatCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * @author Thilina Buddhika
 */
public class ChannelWriterDataHolder {

    private final Logger logger = LogManager.getLogger(ChannelWriterDataHolder.class);

    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024 * 10);

    public void writeData(byte[] payload) {
        //System.out.println("About to write data.");
        synchronized (this) {
            //System.out.println("Acquired the lock!");
            while (byteBuffer.remaining() < (payload.length + 4)) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            byteBuffer.putInt(payload.length).put(payload);
            StatCollector.getInstance().updateStatistics(payload.length);
            this.notifyAll();
            //System.out.println("Done writing. Released lock!");
        }
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

}
