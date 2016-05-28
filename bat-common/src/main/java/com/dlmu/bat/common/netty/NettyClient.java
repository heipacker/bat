package com.dlmu.bat.common.netty;

import com.dlmu.bat.common.NetUtil;
import com.dlmu.bat.common.Serializers;
import com.dlmu.bat.common.codec.Serializer;
import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OneTimeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Netty客户端。
 *
 * @param <T> 交互类型。
 * @author heipacker
 */
public abstract class NettyClient<T> implements Closeable {

    private static Logger log = LoggerFactory.getLogger(NettyClient.class);

    protected volatile Channel channel;

    protected final Bootstrap bootstrap;

    protected volatile boolean closed = false;

    protected final BlockingQueue<T> queue;

    protected final int batchSize;

    protected final Config<T> config;

    private static final EventLoopGroup GROUP = new NioEventLoopGroup(1);

    public NettyClient(BlockingQueue<T> queue, int batchSize, Config<T> config) {
        Preconditions.checkNotNull(queue);
        this.queue = queue;
        this.batchSize = batchSize;
        this.bootstrap = new Bootstrap();
        this.config = config;

        bootstrap.group(GROUP);
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.connectionTimeoutMS);
        bootstrap.channel(NioSocketChannel.class);

        final ConnectionManager connectionManager = new ConnectionManager();
        final Encoder encoder = new Encoder();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("connection", connectionManager);
                int readerIdleMS = NettyClient.this.config.readerIdleMS;
                int writerIdleMS = NettyClient.this.config.writerIdleMS;
                int allIdleMS = NettyClient.this.config.allIdleMS;
                pipeline.addLast("idle", new IdleStateHandler(readerIdleMS, writerIdleMS, allIdleMS, TimeUnit.MILLISECONDS));
                pipeline.addLast("heartbeat", new HeartbeatHandler());
                pipeline.addLast("encoder", encoder);
            }
        });

        connect();
    }

    public boolean isActive() {
        return channel != null && !closed && channel.isActive();
    }

    public void close() {
        this.closed = true;
        if (this.channel != null) {
            this.channel.close();
        }
    }

    public void connect() {
        if (isActive()) return;
        bootstrap.connect(NetUtil.createSocketAddress(config.server));
    }

    @ChannelHandler.Sharable
    private class Encoder extends ChannelOutboundHandlerAdapter {

        @SuppressWarnings("unchecked")
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            ByteBuf buf = ctx.alloc().buffer();
            if (msg instanceof List) {
                List<T> messageList = (List<T>) msg;
                encode(buf, messageList);
                ctx.writeAndFlush(buf, promise);
            } else {
                encode(buf, (T) msg);
            }
        }

        private void encode(ByteBuf buf, List<T> messageList) {
            for (T message : messageList) {
                encode(buf, message);
            }
        }

        /**
         * 先写一个标志为 OTHER_TYPE, 然后将message序列化到buf中
         *
         * @param buf
         * @param message
         */
        private void encode(ByteBuf buf, T message) {
            try {
                buf.writeByte(Serializers.OTHER_TYPE);
                int start = buf.writerIndex();
                buf.writeInt(0);

                config.serializer.serialize(message, new ByteBufOutputStream(buf));
                int stop = buf.writerIndex();
                int len = stop - start;
                buf.writerIndex(start);
                buf.writeInt(len - Serializers.INT_BYTE_SIZE);
                buf.writerIndex(stop);
            } catch (Exception ignored) {

            }
        }

    }

    private class HeartbeatHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
                ctx.writeAndFlush(Serializers.PING.duplicate());
                return;
            }

            if (evt == IdleStateEvent.READER_IDLE_STATE_EVENT) {
                ctx.close();
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    /**
     * 连接管理
     * 1. 第一次尝试连接，然后无法连接到server端，这个时候close会调用，但是inActive不会调用。则用一个定时任务延迟执行重试连接
     * 2. 如果之前已经连接成功，然后由于心跳原因关闭连接，则先会执行close，然后执行inActive
     * 3. 如果server主动关闭，则只有inActive会调用
     */
    @ChannelHandler.Sharable
    private class ConnectionManager extends ChannelDuplexHandler {
        private ScheduledFuture<?> reconnectFuture;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            if (closed) {
                ctx.close();
                return;
            }
            channel = ctx.channel();
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (reconnectFuture != null) {
                reconnectFuture.cancel(false);
            }
            if (closed) return;
            NettyClient.this.connect();
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
            super.close(ctx, future);
            if (closed) return;
            reconnectFuture = ctx.channel().eventLoop().schedule(new OneTimeTask() {
                @Override
                public void run() {
                    if (closed) return;
                    NettyClient.this.connect();
                }
            }, config.closeDelayMS, TimeUnit.MILLISECONDS);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.warn("disconnect from qtracer server: {}", ctx.channel());
            ctx.close();
        }
    }

    public void write(T t) {
        if (!queue.offer(t)) {
            log.info(this.getClass().getSimpleName() + " queue is full, discarded! ");
        }
        this.channel.eventLoop().execute(FLUSH_TASK);
    }

    private Runnable FLUSH_TASK = new Runnable() {
        @Override
        public void run() {
            if (!channel.isWritable()) return;
            if (queue.size() > 0) {
                int size = Math.min(batchSize, queue.size());
                List<T> spans = new ArrayList<T>(size);
                size = queue.drainTo(spans, size);
                if (size > 0) channel.writeAndFlush(spans);
            }
        }
    };

    public static class Config<T> {

        private final String server;

        private final Serializer<T> serializer;

        private final int connectionTimeoutMS;

        private final int readerIdleMS;

        private final int writerIdleMS;

        private final int allIdleMS;

        private final int closeDelayMS;

        public Config(String server, Serializer<T> serializer,
                      int connectionTimeoutMS, int readerIdleMS,
                      int writerIdleMS, int allIdleMS, int closeDelayMS) {
            this.server = server;
            this.serializer = serializer;
            this.connectionTimeoutMS = connectionTimeoutMS;
            this.readerIdleMS = readerIdleMS;
            this.writerIdleMS = writerIdleMS;
            this.allIdleMS = allIdleMS;
            this.closeDelayMS = closeDelayMS;
        }

        public int getConnectionTimeoutMS() {
            return connectionTimeoutMS;
        }

        public Serializer<T> getSerializer() {
            return serializer;
        }

        public String getServer() {
            return server;
        }

        public int getReaderIdleMS() {
            return readerIdleMS;
        }

        public int getWriterIdleMS() {
            return writerIdleMS;
        }

        public int getAllIdleMS() {
            return allIdleMS;
        }

        public int getCloseDelayMS() {
            return closeDelayMS;
        }
    }

    public static class ConfigBuilder<T> {

        private String server;

        private Serializer<T> serializer;

        private int connectionTimeoutMS = 1000;

        private int readerIdleMS = 60000;

        private int writerIdleMS = 30000;

        private int allIdleMS = 30000;

        private int closeDelayMS = 1000;

        public ConfigBuilder() {
        }

        public ConfigBuilder(Config<T> config) {
            this.server = config.server;
            this.serializer = config.serializer;
            this.connectionTimeoutMS = config.connectionTimeoutMS;
            this.readerIdleMS = config.readerIdleMS;
            this.writerIdleMS = config.writerIdleMS;
            this.allIdleMS = config.allIdleMS;
            this.closeDelayMS = config.closeDelayMS;
        }

        public ConfigBuilder<T> setServer(String server) {
            this.server = server;
            return this;
        }

        public ConfigBuilder<T> setSerializer(Serializer<T> serializer) {
            this.serializer = serializer;
            return this;
        }

        public ConfigBuilder<T> setConnectionTimeoutMS(int connectionTimeoutMS) {
            this.connectionTimeoutMS = connectionTimeoutMS;
            return this;
        }

        public ConfigBuilder<T> setReaderIdleMS(int readerIdleMS) {
            this.readerIdleMS = readerIdleMS;
            return this;
        }

        public ConfigBuilder<T> setWriterIdleMS(int writerIdleMS) {
            this.writerIdleMS = writerIdleMS;
            return this;
        }

        public ConfigBuilder<T> setAllIdleMS(int allIdleMS) {
            this.allIdleMS = allIdleMS;
            return this;
        }

        public ConfigBuilder<T> setCloseDelayMS(int closeDelayMS) {
            this.closeDelayMS = closeDelayMS;
            return this;
        }

        public Config<T> build() {
            Preconditions.checkNotNull(server);
            Preconditions.checkNotNull(serializer);
            // 检测
            return new Config<T>(server, serializer, connectionTimeoutMS,
                    readerIdleMS, writerIdleMS, allIdleMS, closeDelayMS);
        }
    }
}
