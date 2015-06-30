package com.thilinamb.highperfcomm.netty.client;

import com.thilinamb.highperfcomm.netty.server.StatCollector;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Random;

/**
 * @author Thilina Buddhika
 */
public class NettyClient {

    private static Random random = new Random(12345);

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage <host> <port>");
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new DataLengthEncoder());

            // Make a new connection.
            ChannelFuture f = b.connect(hostname, port).sync();
            // generate payload and send
            Channel channel = f.channel();
            int i = 0;
            while (true) {
                byte[] payload = getRandomPayload();
                ChannelFuture future = channel.writeAndFlush(payload);
                if (++i % 10000 == 0) {
                    future.sync();
                }
                StatCollector.getInstance().updateStatistics(payload.length);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static byte[] getRandomPayload() {
        byte[] bytes = new byte[1024 * 100];
        random.nextBytes(bytes);
        return bytes;
    }

}
