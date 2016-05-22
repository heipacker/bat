package com.dlmu.bat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class BatServerStartable {

    private static final Logger logger = LoggerFactory.getLogger(BatServerStartable.class);

    private BatServer server;

    private BatServerStartable(ServerConfig serverConfig) {
        server = new BatServer(serverConfig);
    }

    public static BatServerStartable fromProps(Properties serverProps) {
        return new BatServerStartable(ServerConfig.fromProps(serverProps));
    }

    public void shutdown() {
        try {
            server.shutdown();
        } catch (Throwable e) {
            logger.error("Fatal error during BatServerStartable shutdown. Prepare to halt", e);
            // Calling exit() can lead to deadlock as exit() can be called multiple times. Force exit.
            Runtime.getRuntime().halt(1);
        }
    }

    public void startup() {
        try {
            server.startup();
        } catch (Throwable e) {
            logger.error("Fatal error during BatServerStartable startup. Prepare to shutdown", e);
            // BatServer already calls shutdown() internally, so this is purely for logging & the exit code
            System.exit(1);
        }

    }

    public void awaitShutdown() throws InterruptedException {
        server.awaitShutdown();
    }
}
