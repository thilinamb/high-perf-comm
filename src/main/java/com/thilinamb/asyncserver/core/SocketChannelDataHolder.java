package com.thilinamb.asyncserver.core;

import com.thilinamb.asyncserver.core.util.Constants;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Thilina Buddhika
 */
public class SocketChannelDataHolder {

    private ByteBuffer lengthBuffer =  ByteBuffer.allocate(Constants.MESSAGE_LENGTH_BUFFER_SIZE);
    private ByteBuffer dataBuffer;

    private AtomicLong counter = new AtomicLong();

    private int messageSize = -1;

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public ByteBuffer getLengthBuffer(){
        return lengthBuffer;
    }

    public ByteBuffer getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(ByteBuffer dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

    public long incrementCounter(){
        return counter.incrementAndGet();
    }
}
