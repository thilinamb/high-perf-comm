package com.thilinamb.asyncserver.core.server2;

import com.thilinamb.asyncserver.core.util.Constants;

import java.io.IOException;

/**
 * @author Thilina Buddhika
 */
public class Server {
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
            ChannelAcceptor channelAcceptor = new ChannelAcceptor(serverPort, threadPoolSize);
            new Thread(channelAcceptor).start();
        } catch (IOException e) {

        }
        try {
            Thread.sleep(60*60*1000);
        } catch (InterruptedException ignore) {

        }
    }

    private static void printUsage() {
        System.err.println("Usage: -n <reactor-thread-pool size> -p <server port>");
    }
}
