package com.thilinamb.asyncserver.core.task;

import com.thilinamb.asyncserver.core.SocketChannelDataHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author Thilina Buddhika
 */
public class ChannelReaderTask extends Task {

    private final Logger logger = LogManager.getLogger(ChannelReaderTask.class);

    public ChannelReaderTask(SelectionKey selectionKey) {
        super(selectionKey);
    }

    @Override
    public void run() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        SocketChannelDataHolder dataHolder = (SocketChannelDataHolder) selectionKey.attachment();
        synchronized (dataHolder) {
            if(logger.isDebugEnabled()){
                try {
                    logger.debug("Running the ChannelReader task to read from " +
                            socketChannel.getRemoteAddress());
                } catch (IOException ignore) {

                }
            }
            int messageLength = dataHolder.getMessageSize();

            if(messageLength == -1){
                ByteBuffer lengthBuffer = dataHolder.getLengthBuffer();
                try {
                    int length = socketChannel.read(lengthBuffer);
                    if(logger.isDebugEnabled()){
                        try {
                            logger.debug("Read " + length + " bytes  into length buffer from " +
                                    socketChannel.getRemoteAddress());
                        } catch (IOException ignore) {

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(!lengthBuffer.hasRemaining()){
                    lengthBuffer.flip();
                    messageLength = lengthBuffer.getInt();
                    dataHolder.setMessageSize(messageLength);
                    lengthBuffer.clear();
                    if(logger.isDebugEnabled()){
                        try {
                            logger.debug("Read the complete message length. Length: " + messageLength + ", from: " +
                                    socketChannel.getRemoteAddress());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    return;
                }
            }

            if(dataHolder.getDataBuffer() == null){
                dataHolder.setDataBuffer(ByteBuffer.allocate(messageLength));
            }

            ByteBuffer dataBuffer = dataHolder.getDataBuffer();
            try {
                int length = socketChannel.read(dataBuffer);
                if(logger.isDebugEnabled()){
                    try {
                        logger.debug("Read " + length + " bytes  into data buffer from " +
                                socketChannel.getRemoteAddress());
                    } catch (IOException ignore) {

                    }
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            if(!dataBuffer.hasRemaining()){
                byte[] bytes = new byte[messageLength];
                dataBuffer.flip();
                dataBuffer.get(bytes);
                dataHolder.setDataBuffer(null);
                long counter;
                if(( counter = dataHolder.incrementCounter()) % 100000 == 0){
                    logger.info("Received " + counter + " messages.");
                }
                dataHolder.setMessageSize(-1);
            }
        }

    }
}
