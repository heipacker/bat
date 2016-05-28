package com.dlmu.bat.server;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.codec.Deserializer;
import com.dlmu.bat.common.conf.ConfigConstants;
import com.dlmu.bat.common.netty.NettyServer;
import com.dlmu.bat.plugin.conf.Configuration;
import com.dlmu.bat.store.StoreService;
import com.dlmu.bat.store.impl.HBaseStoreServiceImpl;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.I0Itec.zkclient.ZkClient;
import org.hbase.async.HBaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class BatServer {

    private static final Logger logger = LoggerFactory.getLogger(BatServer.class);

    private AtomicBoolean startupComplete = new AtomicBoolean(false);
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private AtomicBoolean isStartingUp = new AtomicBoolean(false);

    private CountDownLatch shutdownLatch = new CountDownLatch(1);

    private Configuration configuration;

    private SpanNettyServer spanNettyServer;

    private StoreService storeService;

    public BatServer(Configuration configuration) {
        this.configuration = configuration;
    }

    public void startup() {
        try {
            logger.info("starting");

            if (isShuttingDown.get())
                throw new IllegalStateException("Kafka server is still shutting down, cannot re-start!");

            if (startupComplete.get())
                return;

            boolean canStartup = isStartingUp.compareAndSet(false, true);
            if (canStartup) {
                ZkClient zkClient = new ZkClient(configuration.get(ConfigConstants.ZK_ADDRESS_KEY, ConfigConstants.DEFAULT_ZK_ADDRESS));
                String hbaseZkAddress = configuration.get(ConfigConstants.HBASE_ZK_ADDRESS_KEY);
                Preconditions.checkArgument(!Strings.isNullOrEmpty(hbaseZkAddress));
                String hbasePath = configuration.get(ConfigConstants.HBASE_ZK_PATH_KEY);
                Preconditions.checkArgument(!Strings.isNullOrEmpty(hbasePath));
                HBaseClient hBaseClient = new HBaseClient(hbaseZkAddress, hbasePath);
                String batTable = configuration.get(ConfigConstants.HBASE_BAT_TRACE_TABLE_KEY);
                Preconditions.checkArgument(!Strings.isNullOrEmpty(batTable));

                storeService = new HBaseStoreServiceImpl(hBaseClient, batTable);
                NettyServer.Processor processor = new NettyServer.DecoderProcess<BaseSpan>(new Deserializer<BaseSpan>() {
                    @Override
                    public BaseSpan deserialize(byte[] sources) throws Exception {
                        return new BaseSpan(sources);
                    }
                }) {
                    /**
                     * 解码成功。
                     *
                     * @param sources    字节流。
                     * @param result     字节流解序列化后的实例。
                     * @param remoteInfo 远程客户端信息。
                     */
                    @Override
                    public void completed(byte[] sources, BaseSpan result, String remoteInfo) {
                        storeService.storeSpan(result);
                    }

                    /**
                     * 解码失败或调用completed失败。
                     *
                     * @param sources    字节流。
                     * @param exc        字节流解序列化失败后的异常。
                     * @param remoteInfo 远程客户端信息。
                     */
                    @Override
                    public void failed(byte[] sources, Throwable exc, String remoteInfo) {
                        logger.error("decode error, sources{}, remoteInfo:{}", sources, remoteInfo, exc);
                    }
                };
                int port = configuration.getInt(ConfigConstants.SERVER_PORT_KEY, ConfigConstants.DEFAULT_SERVER_PORT);
                this.spanNettyServer = new SpanNettyServer(zkClient, Collections.singletonList(processor), port);
                this.spanNettyServer.init();
            }
        } catch (Throwable e) {
            logger.error("Fatal error during KafkaServer startup. Prepare to shutdown", e);
            isStartingUp.set(false);
            shutdown();
            throw Throwables.propagate(e);
        }
    }

    public void shutdown() {
        try {
            logger.info("shutting down");

            if (isStartingUp.get())
                throw new IllegalStateException("Kafka server is still starting up, cannot shut down!");

            boolean canShutdown = isShuttingDown.compareAndSet(false, true);
            if (canShutdown && shutdownLatch.getCount() > 0) {
                this.spanNettyServer.destroy();
            }
        } catch (Throwable e) {
            logger.error("shut down error", e);
            throw Throwables.propagate(e);
        }
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
}
