package com.thilinamb.highperfcomm.netty.client;

import com.thilinamb.highperfcomm.netty.server.StatCollector;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;

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

        EventLoopGroup group = new EpollEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            b.group(group)
                    .channel(EpollSocketChannel.class)
                    .handler(new DataLengthEncoder());

            // Make a new connection.
            ChannelFuture f = b.connect(hostname, port).sync();
            // generate payload and send
            Channel channel = f.channel();
            int i = 0;
            while (true) {
                byte[] payload = getRandomPayload();
                ChannelFuture future = channel.write(payload);
                if (++i % 1000 == 0) {
                    channel.flush();
                }
                if(i % 100000 == 0){
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
        byte[] bytes = new byte[250];
        random.nextBytes(bytes);
        return bytes;
    }

}
