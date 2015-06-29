package com.thilinamb.asyncserver.core.nclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @author Thilina Buddhika
 */
public class IOReactor2 implements Runnable{
    private final Logger logger = LogManager.getLogger(IOReactor2.class);
    private final Selector selector;

    private List<SocketChannelHolder> unInitializedChannels = Collections.synchronizedList(
            new ArrayList<SocketChannelHolder>());

    private List<SocketChannelHolder> initializedChannels = Collections.synchronizedList(
            new ArrayList<SocketChannelHolder>());

    private class SocketChannelHolder {
        private final SocketChannel socketChannel;
        private final ChannelWriterDataHolder dataHolder;

        public SocketChannelHolder(SocketChannel socketChannel, ChannelWriterDataHolder dataHolder) {
            this.socketChannel = socketChannel;
            this.dataHolder = dataHolder;
        }
    }

    public IOReactor2() throws IOException {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            /*if(!unInitializedChannels.isEmpty()){
                processUnInitializedChannels();
            }*/

            for(SocketChannelHolder socketChannelHolder: unInitializedChannels){
                write(socketChannelHolder.socketChannel, socketChannelHolder.dataHolder);
            }

            /*int numOfKeys = 0;

            try {
                numOfKeys = selector.select();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            // no new selected keys. start the loop again.
            if (numOfKeys == 0) {
                continue;
            }

            Set keys = selector.selectedKeys();
            Iterator it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey selectionKey = (SelectionKey) it.next();
                it.remove();
                if (!selectionKey.isValid()) {
                    continue;
                } else if (selectionKey.isWritable()) {
                    write(selectionKey);
                }
            }
            */

        }
    }

    public void registerChannel(SocketChannel socketChannel, ChannelWriterDataHolder dataHolder)
            throws ClosedChannelException {
        unInitializedChannels.add(new SocketChannelHolder(socketChannel, dataHolder));
        selector.wakeup();
    }

    private void processUnInitializedChannels() {
        for (SocketChannelHolder channel : unInitializedChannels) {
            try {
                channel.socketChannel.register(selector, SelectionKey.OP_WRITE, channel.dataHolder);
            } catch (ClosedChannelException e) {
                logger.error(e.getMessage(), e);
            }
        }
        unInitializedChannels.clear();
    }

    private void write(SocketChannel socketChannel, ChannelWriterDataHolder dataHolder) {
        // SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        // ChannelWriterDataHolder dataHolder = (ChannelWriterDataHolder) selectionKey.attachment();
        synchronized (dataHolder) {
            ByteBuffer byteBuffer = dataHolder.getByteBuffer();
            if (byteBuffer.remaining() < byteBuffer.capacity()) {
                byteBuffer.flip();
                try {
                    socketChannel.write(byteBuffer);
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
