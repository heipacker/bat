package com.dlmu.bat.server;

import com.dlmu.bat.common.conf.ConfigConstants;
import com.dlmu.bat.common.netty.NettyServer;
import com.dlmu.bat.plugin.conf.Configuration;
import com.google.common.base.Throwables;
import org.I0Itec.zkclient.ZkClient;
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
                NettyServer.Processor processor = new NettyServer.Processor() {
                    @Override
                    public void process(byte[] sources, String remoteInfo) {

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
