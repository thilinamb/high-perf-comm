package com.thilinamb.asyncserver.core.server2;

import com.thilinamb.asyncserver.core.util.ServerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Thilina Buddhika
 */
public class ChannelAcceptor implements Runnable {

    private final Logger logger = LogManager.getLogger(ChannelAcceptor.class);
    private final Selector selector;
    private final IOReactor[] ioReactors;
    private int lastAssignedReactor = 0;
    private final int port;

    public ChannelAcceptor(int port, int reactorPoolSize) throws IOException {
        try {
            this.selector = Selector.open();
            this.port = port;
            // initialize the IOReactors
            ioReactors = new IOReactor[reactorPoolSize];
            for(int i=0; i < reactorPoolSize; i++){
                IOReactor ioReactor = new IOReactor(i);
                new Thread(ioReactor).start();
                ioReactors[i] = ioReactor;
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void run() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ServerUtil.getHostInetAddress(),
                    port);
            serverSocket.bind(inetSocketAddress);
            // register the selector
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            logger.info("Server started on: " + inetSocketAddress.getHostString() + ":" + port);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        while (!Thread.interrupted()) {
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
                if (key.isAcceptable()) {
                    // Handling accept connections in the selector thread itself.
                    try {

                        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                        if (socketChannel != null) {
                            lastAssignedReactor = ++lastAssignedReactor % ioReactors.length;
                            IOReactor ioReactor = ioReactors[lastAssignedReactor];
                            ioReactor.registerSocketChannel(socketChannel);
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

}
