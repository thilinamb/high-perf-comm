package com.thilinamb.highperfcomm.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Thilina Buddhika
 */
public class ServerHandler extends SimpleChannelInboundHandler<byte[]> {

    private final Logger logger = LogManager.getLogger(ServerHandler.class);
    private StatCollector statCollector = StatCollector.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("New incoming channel is accepted.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {
        statCollector.updateStatistics(bytes.length);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        ctx.close();
}
}
