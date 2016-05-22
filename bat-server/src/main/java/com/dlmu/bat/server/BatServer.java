package com.dlmu.bat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ServerConfig serverConfig;

    public BatServer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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
                //todo add startup code
            }
        } catch (Throwable e) {
            logger.error("Fatal error during KafkaServer startup. Prepare to shutdown", e);
            isStartingUp.set(false);
            shutdown();
            throw e;
        }
    }

    public void shutdown() {
        try {
            logger.info("shutting down");

            if (isStartingUp.get())
                throw new IllegalStateException("Kafka server is still starting up, cannot shut down!");

            boolean canShutdown = isShuttingDown.compareAndSet(false, true);
            if (canShutdown && shutdownLatch.getCount() > 0) {
                //todo add shutdown code
            }
        } catch (Throwable e) {

        }
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
}
