package com.thilinamb.asyncserver.core.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Author: Thilina
 * Date: 3/8/14
 */
public class Client {

    private Selector selector;
    private final String serverHost;
    private final int serverPort;
    private final int buffSize;
    private SocketChannel socketChannel;
    private WriteWorker writeWorker;
    private ReadWorker readWorker;
    private List<String> hashCodes = new LinkedList<String>();

    private Logger logger = LogManager.getLogger(Client.class);

    public Client(String serverHost, int serverPort, int buffSize) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.buffSize = buffSize;
    }


    public boolean initialize() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        try {
            // create the socket channel.
            socketChannel = SocketChannel.open();
            // configure it to be non-blocking
            socketChannel.configureBlocking(false);
            SocketAddress socketAddress = new InetSocketAddress(serverHost, serverPort);
            socketChannel.connect(socketAddress);
            logger.info("Successfully connected to " + socketAddress);

            // start the write worker thread.
            writeWorker = new WriteWorker(socketChannel, buffSize);
            writeWorker.start();

            // create and start the PayloadGenerator
            PayloadGenerator payloadGen = new PayloadGenerator(writeWorker);
            payloadGen.start();

            // start the read worker
            readWorker = new ReadWorker(socketChannel);
            readWorker.start();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    public void start() {
        try {
            // register for connect.
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            while (true) {

                // now check for new keys
                int numOfKeys = selector.select();
                // no new selected keys. start the loop again.
                if (numOfKeys == 0) {
                    continue;
                }

                // get the keys
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    if (key.isValid() && key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isValid() && key.isReadable()) {
                        readWorker.wakeUp();
                    } else if (key.isValid() && key.isWritable()) {
                        writeWorker.wakeUp();
                    }
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleConnect(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.finishConnect()) {
                SelectionKey readKey = channel.register(selector, SelectionKey.OP_WRITE);
                channel.register(selector, readKey.interestOps() | SelectionKey.OP_READ);
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

    public static void main(String[] args) {
        // Check if the required arguments are provided.
        if (args.length < 3) {
            if (args.length < 3) {
                System.out.println("Usage <server-hostname> <port> <buffSize>");
                System.exit(-1);
            }
        }

        // parse the input arguments.
        String serverHost = args[0];
        int port = Integer.parseInt(args[1]);
        int buffSize = Integer.parseInt(args[2]);

        // Create the client instance and initialize
        Client client = new Client(serverHost, port, buffSize);
        boolean initStatus = client.initialize();

        // Check the initialization status.
        if (!initStatus) {
            System.err.println("Client initialization Failed!");
            System.exit(-1);
        }

        client.start();

    }

}
