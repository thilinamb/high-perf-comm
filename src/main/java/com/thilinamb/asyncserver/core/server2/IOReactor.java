package com.thilinamb.asyncserver.core.server2;

import com.thilinamb.asyncserver.core.SocketChannelDataHolder2;
import com.thilinamb.asyncserver.core.client.StatCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;


/**
 * @author Thilina Buddhika
 */
public class IOReactor implements Runnable {

    private final Logger logger = LogManager.getLogger(IOReactor.class);
    private final Selector selector;
    private final int reactorId;

    private final List<SocketChannel> unprocessedChannels = Collections.synchronizedList(new ArrayList<SocketChannel>());

    public IOReactor(int reactorId) throws IOException {
        this.reactorId = reactorId;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            if(!unprocessedChannels.isEmpty()) {
                processNewChannels();
            }

            int numOfKeys = 0;

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
                } else if (selectionKey.isReadable()) {
                    read(selectionKey);
                }
            }
        }
    }

    private void processNewChannels() {
        synchronized (unprocessedChannels) {
            for (SocketChannel socketChannel : unprocessedChannels) {
                try {
                    // configure it non-blocking
                    socketChannel.configureBlocking(false);
                    SocketChannelDataHolder2 socketChannelDataHolder = new SocketChannelDataHolder2();
                    // register read/write interests
                    socketChannel.register(selector, SelectionKey.OP_READ, socketChannelDataHolder);
                    logger.info("[IOReactor - " + this.reactorId + "] Accepted a new connection from " +
                            socketChannel.getRemoteAddress());

                } catch (IOException e) {

                }
            }
            unprocessedChannels.clear();
        }
    }

    public void registerSocketChannel(SocketChannel socketChannel) {
        synchronized (unprocessedChannels) {
            unprocessedChannels.add(socketChannel);
        }
        selector.wakeup();
    }

    public void read(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        SocketChannelDataHolder2 dataHolder = (SocketChannelDataHolder2) selectionKey.attachment();
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
