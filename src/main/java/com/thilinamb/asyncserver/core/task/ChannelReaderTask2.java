package com.thilinamb.asyncserver.core.task;

import com.thilinamb.asyncserver.core.SocketChannelDataHolder2;
import com.thilinamb.asyncserver.core.client.StatCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author Thilina Buddhika
 */
public class ChannelReaderTask2 extends Task {

    private final Logger logger = LogManager.getLogger(ChannelReaderTask2.class);

    public ChannelReaderTask2(SelectionKey selectionKey) {
        super(selectionKey);
    }

    @Override
    public void run() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        SocketChannelDataHolder2 dataHolder = (SocketChannelDataHolder2) selectionKey.attachment();
        synchronized (dataHolder) {
            // read data
            ByteBuffer dataBuffer = dataHolder.getDataBuffer();
            try {
                socketChannel.read(dataBuffer);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            int bufferContentLength = dataBuffer.position();
            dataBuffer.flip();
            // read messages from the buffer
            while (true) {
                int messageLength = dataHolder.getMessageSize();
                if (messageLength == -1) { // the length is not initialized
                    if ((bufferContentLength - dataBuffer.position()) >= 4) {
                        messageLength = dataBuffer.getInt();
                        dataHolder.setMessageSize(messageLength);
                        dataHolder.setCurrentMessage(new byte[messageLength]);
                        dataHolder.setMessageOffSet(0);
                    } else {
                        // not enough data for the length. wait till next invocation.
                        break;
                    }
                }

                byte[] currentMessage = dataHolder.getCurrentMessage();
                int currentOffset = dataHolder.getMessageOffSet();
                if ((bufferContentLength - dataBuffer.position()) >= (messageLength - currentOffset)) {
                    // read the complete message
                    dataBuffer.get(currentMessage, currentOffset, (messageLength - currentOffset));
                    StatCollector.getInstance().updateStatistics(messageLength);
                    // prepare for the next message
                    dataHolder.setMessageOffSet(0);
                    dataHolder.setMessageSize(-1);
                    //System.out.println("Message Received: " + new String(currentMessage));
                } else {
                    // read as much as possible
                    int bytesToRead = bufferContentLength - dataBuffer.position();
                    dataBuffer.get(currentMessage, currentOffset, bytesToRead);
                    dataHolder.setMessageOffSet(currentOffset + bytesToRead);
                    break;
                }
            }
            dataBuffer.compact();
        }
    }
}
