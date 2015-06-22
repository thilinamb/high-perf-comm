package com.thilinamb.asyncserver.core;

import com.thilinamb.asyncserver.core.util.Constants;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Thilina Buddhika
 */
public class SocketChannelDataHolder2 {
    private ByteBuffer dataBuffer = ByteBuffer.allocate(Constants.READ_BUFFER_SIZE);
    private int readPosition = 0;
    private int messageOffSet = 0;
    private byte[] currentMessage;

    private AtomicLong counter = new AtomicLong();

    private int messageSize = -1;

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
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

    public int getReadPosition() {
        return readPosition;
    }

    public void setReadPosition(int readPosition) {
        this.readPosition = readPosition;
    }

    public int getMessageOffSet() {
        return messageOffSet;
    }

    public void setMessageOffSet(int messageOffSet) {
        this.messageOffSet = messageOffSet;
    }

    public byte[] getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(byte[] currentMessage) {
        this.currentMessage = currentMessage;
    }
}
