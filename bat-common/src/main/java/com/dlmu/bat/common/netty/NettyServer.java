package com.dlmu.bat.common.netty;

import com.dlmu.bat.common.Serializers;
import com.dlmu.bat.common.codec.Deserializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Netty服务端。
 *
 * @author heipacker
 */
public class NettyServer {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ServerBootstrap bootstrap;

    protected final int parentThreadSize;
    protected final int childThreadSize;

    protected final Processor[] processors;

    public NettyServer(int parentThreadSize, int childThreadSize, Processor... processors) {
        this.processors = processors;

        this.parentThreadSize = parentThreadSize;
        this.childThreadSize = childThreadSize;

        bootstrap = new ServerBootstrap();
        bootstrap.group(new NioEventLoopGroup(parentThreadSize), new NioEventLoopGroup(childThreadSize));
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        //基本上不向client发送数据
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1024);

        bootstrap.channel(NioServerSocketChannel.class);

        final ExpiredHandler expiredHandler = new ExpiredHandler();

        final ProcessHandler processHandler = new ProcessHandler();

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //90秒如果还没收到client的数据，则任务client断掉了连接，则关闭连接
                //20秒如果没有给client写数据，则发一次ping
                pipeline.addLast("idle", new IdleStateHandler(90, 20, 0));
                pipeline.addLast("expire", expiredHandler);
                pipeline.addLast("decoder", new Decoder());
                pipeline.addLast("processor", processHandler);
            }
        });
    }

    public ChannelFuture bind(int bindPort) {
        return bootstrap.bind(bindPort);
    }

    public void destroy() throws Exception {
        bootstrap.group().shutdownGracefully();
        bootstrap.childGroup().shutdownGracefully();
    }

    /**
     * 扩展方案，解析失败会调用。
     *
     * @param throwable 异常。
     */
    protected void postProcessDecode(Throwable throwable, String remoteInfo) {

    }

    /**
     * 扩展方案，解析成功会调用。
     *
     * @param readableBytes 传输字节大小。
     */
    protected void postProcessDecode(int readableBytes, String remoteInfo) {

    }

    /**
     * 处理器。
     *
     * @author tianpeng.li
     */
    public static interface Processor {

        void process(byte[] sources, String remoteInfo);

    }

    /**
     * 解码处理器。
     *
     * @param <T> 解码类型。
     * @author tianpeng.li
     */
    public static abstract class DecoderProcess<T> implements Processor {

        private final Deserializer<T> deserializer;

        public DecoderProcess(Deserializer<T> deserializer) {
            this.deserializer = deserializer;
        }

        @Override
        public final void process(byte[] sources, String remoteInfo) {
            try {
                T result = deserializer.deserialize(sources);
                completed(sources, result, remoteInfo);
            } catch (Throwable e) {
                failed(sources, e, remoteInfo);
            }
        }

        /**
         * 解码成功。
         *
         * @param sources    字节流。
         * @param result     字节流解序列化后的实例。
         * @param remoteInfo 远程客户端信息。
         */
        public abstract void completed(byte[] sources, T result, String remoteInfo);

        /**
         * 解码失败或调用completed失败。
         *
         * @param sources    字节流。
         * @param exc        字节流解序列化失败后的异常。
         * @param remoteInfo 远程客户端信息。
         */
        public abstract void failed(byte[] sources, Throwable exc, String remoteInfo);

    }

    private final class Decoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            in.markReaderIndex();
            byte type = in.readByte();
            if (Serializers.isOther(type)) {
                if (in.readableBytes() < Serializers.INT_BYTE_SIZE) {
                    in.resetReaderIndex();
                    return;
                }
                int len = in.readInt();
                if (len <= 0) {
                    return;
                }
                int readableBytes = in.readableBytes();
                if (readableBytes >= len) {
                    byte[] data = new byte[len];
                    in.readBytes(data);
                    out.add(data);
                } else if (readableBytes < 5 * 1024 * 1024) {      // 否则可能产生当前ByteBuf不断增长，这里假设单个span最大5mb
                    in.resetReaderIndex();
                }
            } else if (Serializers.isPing(type)) {
                ctx.writeAndFlush(Serializers.OK.duplicate());
            }
        }

        @Override
        protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            if (in == Unpooled.EMPTY_BUFFER) return;
            decode(ctx, in, out);
        }
    }

    @ChannelHandler.Sharable
    private final class ProcessHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof byte[])) {
                logger.info("error type {}", msg);
                return;
            }

            String remoteInfo = ctx.channel().remoteAddress().toString();
            try {
                byte[] data = (byte[]) msg;
                // processor本身已处理过异常，因此，只有上边失败的情况下才会进入catch块
                for (Processor processor : processors) {
                    processor.process(data, remoteInfo);
                }
                postProcessDecode(data.length, remoteInfo);    // 不考虑本身异常。

            } catch (Throwable e) {
                // 业务端未采用客户端调用，传输的字节码才有问题
                logger.error("解析数据失败, 客户端IP: {}, 信息: {}", ctx.channel().remoteAddress(), e.getMessage(), e);
                postProcessDecode(e, remoteInfo);         // 不考虑本身异常。
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("process span error: {}", ctx.channel(), cause);
        }
    }

    @ChannelHandler.Sharable
    private class ExpiredHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == IdleStateEvent.READER_IDLE_STATE_EVENT) {
                logger.warn("remote ip Expired {}", ctx.channel().remoteAddress());
                ctx.close();
            } else if (evt == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
                ctx.writeAndFlush(Serializers.OK.duplicate());
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("one channel registry to server ip is {}", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.info("one channel disconnect from server ip is {}", ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }
    }
}
