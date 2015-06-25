package com.thilinamb.asyncserver.core.nclient;

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

/**
 * @author Thilina Buddhika
 */
public class ChannelConnector implements Runnable {

        private final Logger logger = LogManager.getLogger(ChannelConnector.class);

        private final Queue<PendingConnection> pendingConnections = new PriorityQueue<>();
        private final Selector selector;

        private final Map<String, ChannelWriterDataHolder> connections = new ConcurrentHashMap<>();
        private final IOReactor[] ioReactors;
        private int lastUsedReactor = 0;

        private class PendingConnection {
            private final String hostName;
            private final int port;
            private final SocketChannel socketChannel;

            public PendingConnection(String hostName, int port, SocketChannel socketChannel) {
                this.hostName = hostName;
                this.port = port;
                this.socketChannel = socketChannel;
            }
        }

        public ChannelConnector(int reactorCount) throws IOException {
            try {
                selector = Selector.open();
                ioReactors = new IOReactor[reactorCount];

                for(int i = 0; i < reactorCount; i++){
                    IOReactor reactor = new IOReactor();
                    Thread reactorThread = new Thread(reactor);
                    reactorThread.setName("WriterIOReactor-" + i);
                    reactorThread.start();
                    ioReactors[i] = reactor;
                }

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
                    //System.out.println("ChannelSelector is running. Num. Keys: " + numOfKeys);
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
                IOReactor reactor = ioReactors[lastUsedReactor++ % ioReactors.length];
                reactor.registerChannel(pendingConnection.socketChannel, dataHolder);
                connections.put(pendingConnection.hostName + ":" + pendingConnection.port, dataHolder);
                // deregister
                //channel.keyFor(selector).cancel();
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
