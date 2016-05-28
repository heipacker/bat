package com.dlmu.bat.server;

import com.dlmu.bat.plugin.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class BatServerStartable {

    private static final Logger logger = LoggerFactory.getLogger(BatServerStartable.class);

    private BatServer batServer;

    private BatServerStartable(Configuration configuration) {
        batServer = new BatServer(configuration);
    }

    public static BatServerStartable fromConfig(Configuration configuration) {
        return new BatServerStartable(configuration);
    }

    public void shutdown() {
        try {
            batServer.shutdown();
        } catch (Throwable e) {
            logger.error("Fatal error during BatServerStartable shutdown. Prepare to halt", e);
            // Calling exit() can lead to deadlock as exit() can be called multiple times. Force exit.
            Runtime.getRuntime().halt(1);
        }
    }

    public void startup() {
        try {
            batServer.startup();
        } catch (Throwable e) {
            logger.error("Fatal error during BatServerStartable startup. Prepare to shutdown", e);
            // BatServer already calls shutdown() internally, so this is purely for logging & the exit code
            System.exit(1);
        }

    }

    public void awaitShutdown() throws InterruptedException {
        batServer.awaitShutdown();
    }
}
