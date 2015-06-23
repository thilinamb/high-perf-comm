package com.thilinamb.asyncserver.core.client2.test;

import com.thilinamb.asyncserver.core.client2.ChannelWriterDataHolder;
import com.thilinamb.asyncserver.core.client2.ClientIOThread;

import java.io.IOException;

/**
 * @author Thilina Buddhika
 */
public class Client2 {
    public static void main(String[] args) {
        // Check if the required arguments are provided.
        if (args.length < 3) {
            if (args.length < 3) {
                System.out.println("Usage <server-hostname> <port> <client-thread-count> <buffSize>");
                System.exit(-1);
            }
        }

        // parse the input arguments.
        String serverHost = args[0];
        int port = Integer.parseInt(args[1]);
        int clientThreadCount = Integer.parseInt(args[2]);
        int payLoadGenerators = Integer.parseInt(args[3]);

        try {
            ClientIOThread ioThread = new ClientIOThread(clientThreadCount);
            new Thread(ioThread).start();

            ioThread.addNewConnection(serverHost, port);
            ChannelWriterDataHolder dataHolder = null;
            while ((dataHolder = ioThread.getDataHolder(serverHost, port)) == null){
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Starting to generated load..");
            for (int i = 0; i < payLoadGenerators; i++) {
                new Thread(new PayloadGenerator(dataHolder)).start();
            }

            try {
                Thread.sleep(60 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
