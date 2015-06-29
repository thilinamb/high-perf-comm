package com.thilinamb.asyncserver.core;

import com.thilinamb.asyncserver.core.exception.StartupException;
import com.thilinamb.asyncserver.core.exception.ThreadPoolException;
import com.thilinamb.asyncserver.core.task.ChannelReaderTask2;
import com.thilinamb.asyncserver.core.util.Constants;
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
@SuppressWarnings("unusedclass")
public class Server {

    private final int serverPort;
    private final Logger logger = LogManager.getLogger(Server.class.getName());

    public Server(int serverPort, int threadPoolSize) {
        this.serverPort = serverPort;
        ThreadPool.initialize(threadPoolSize);
    }

    public void start() throws StartupException {
        ThreadPool threadPool;
        Selector selector;
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ServerUtil.getHostInetAddress(),
                    this.serverPort);
            serverSocket.bind(inetSocketAddress);
            logger.info("Server started on " + inetSocketAddress.getHostName() + ":" + this.serverPort);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            threadPool = ThreadPool.getInstance();

        } catch (IOException | ThreadPoolException e) {
            logger.error(e.getMessage(), e);
            throw new StartupException(e.getMessage(), e);
        }

        while (true) {
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
                            // configure it non-blocking
                            socketChannel.configureBlocking(false);
                            SocketChannelDataHolder2 socketChannelDataHolder = new SocketChannelDataHolder2();
                            // register read/write interests
                            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                                    socketChannelDataHolder);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Accepted a new connection from " + socketChannel.getRemoteAddress());
                            }
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                } else if (key.isReadable()) {
                    // handle read tasks to job queue
                    ChannelReaderTask2 readTask = new ChannelReaderTask2(key);
                    threadPool.submitTask(readTask);
                } /*else if (key.isWritable()) {
                    // handle write tasks to job queue
                    ChannelWriterTask writeTask = new ChannelWriterTask(key);
                    threadPool.submitTask(writeTask);
                }*/
            }
        }

    }

    public static void main(String[] args) {
        if (args.length > 0 && args.length != 4) {
            printUsage();
            return;
        }
        int threadPoolSize = Constants.DEFAULT_THREAD_POOL_SIZE;
        int serverPort = Constants.DEFAULT_SERVER_PORT;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-n")) {
                    threadPoolSize = Integer.parseInt(args[i + 1]);
                }
                if (arg.equals("-p")) {
                    serverPort = Integer.parseInt(args[i + 1]);
                }
            }
        } catch (RuntimeException e) {
            printUsage();
            return;
        }

        try {
            Server server = new Server(serverPort, threadPoolSize);
            server.start();
        } catch (StartupException e) {

        }
    }

    private static void printUsage() {
        System.err.println("Usage: -n <thread-pool size> -p <server port>");
    }
}
