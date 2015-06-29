package com.thilinamb.asyncserver.core.client2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Thilina Buddhika
 */
public class ClientIOThread implements Runnable {

    private Logger logger = LogManager.getLogger(ClientIOThread.class);

    private final Queue<PendingConnection> pendingConnections = new PriorityQueue<>();
    private Selector selector;

    private Map<String, ChannelWriterDataHolder> connections = new ConcurrentHashMap<>();
    private ExecutorService threadPool;

    private class PendingConnection {
        private String hostName;
        private int port;
        private SocketChannel socketChannel;

        public PendingConnection(String hostName, int port, SocketChannel socketChannel) {
            this.hostName = hostName;
            this.port = port;
            this.socketChannel = socketChannel;
        }
    }

    public ClientIOThread(int threadPoolSize) throws IOException {
        try {
            selector = Selector.open();
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            processPendingConnections();
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
                SelectionKey key = (SelectionKey) it.next();
                it.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isConnectable()) {
                    handleConnect(key);
                } else if (key.isWritable()) {
                    // handle write tasks to job queue
                    //System.out.println("Channel is writable!");
                    ChannelWriterTask writeTask = new ChannelWriterTask(key);
                    threadPool.execute(writeTask);
                }
            }
        }

    }

    private void processPendingConnections(){
        synchronized (pendingConnections){
            for(PendingConnection pendingConnection: pendingConnections){
                try {
                    pendingConnection.socketChannel.register(selector, SelectionKey.OP_CONNECT, pendingConnection);
                } catch (ClosedChannelException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            pendingConnections.clear();
        }
    }

    private void handleConnect(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.finishConnect()) {
                PendingConnection pendingConnection = (PendingConnection)key.attachment();
                ChannelWriterDataHolder dataHolder = new ChannelWriterDataHolder();
                channel.register(selector, SelectionKey.OP_WRITE, dataHolder);
                connections.put(pendingConnection.hostName + ":" + pendingConnection.port, dataHolder);
                logger.info("Connection established with " +
                        pendingConnection.hostName + ":" + pendingConnection.port);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            try {
                channel.close();
                key.cancel();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    public void addNewConnection(String serverHost, int serverPort){
        try {
            SocketChannel socketChannel = SocketChannel.open();
            // configure it to be non-blocking
            socketChannel.configureBlocking(false);
            SocketAddress socketAddress = new InetSocketAddress(serverHost, serverPort);
            socketChannel.connect(socketAddress);
            PendingConnection pendingSocketChannel  = new PendingConnection(
                    serverHost, serverPort, socketChannel);
            synchronized (pendingConnections){
                pendingConnections.add(pendingSocketChannel);
            }
            selector.wakeup();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public ChannelWriterDataHolder getDataHolder(String hostName, int port){
        return connections.get(hostName + ":" + port);
    }
}
