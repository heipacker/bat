package com.dlmu.bat.server.zk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class ZKClientCache {

    private static final Logger logger = LoggerFactory.getLogger(ZKClientCache.class);

    private static final Map<String, ZKClient> CACHE = new HashMap<String, ZKClient>();

    public synchronized static ZKClient get(String address) {
        logger.info("Get zkclient for {}", address);
        ZKClient client = CACHE.get(address);
        if (client == null) {
            CACHE.put(address, new ZKClientImpl(address));
        }
        client = CACHE.get(address);
        client.incrementReference();
        return client;
    }

    public static ZKClient getFixed(String address) {
        return get(address);
    }
}
