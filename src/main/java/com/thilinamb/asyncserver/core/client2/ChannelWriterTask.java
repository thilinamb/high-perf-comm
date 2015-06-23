package com.thilinamb.asyncserver.core.client2;

import com.thilinamb.asyncserver.core.task.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author Thilina Buddhika
 */
public class ChannelWriterTask extends Task {

    private final Logger logger = LogManager.getLogger(ChannelWriterTask.class);

    public ChannelWriterTask(SelectionKey selectionKey) {
        super(selectionKey);
    }

    @Override
    public void run() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelWriterDataHolder dataHolder = (ChannelWriterDataHolder) selectionKey.attachment();
        synchronized (dataHolder) {
            ByteBuffer byteBuffer = dataHolder.getByteBuffer();
            if (byteBuffer.remaining() < byteBuffer.capacity()) {
                byteBuffer.flip();
                try {
                    socketChannel.write(byteBuffer);
                    //System.out.println("Writer Task: Wrote data!");
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                if (!byteBuffer.hasRemaining()) {
                    byteBuffer.clear();
                } else {
                    byteBuffer.compact();
                }
                dataHolder.notifyAll();
            }
        }
    }

}
