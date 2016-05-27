package com.dlmu.bat.common.conf;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class ConfigConstants {
    /**
     * The configuration key to use for process id
     */
    public static final String TRACER_ID_KEY = "dTraceClient.id";
    /**
     * The default dTraceClient ID to use if no other ID is configured.
     */
    public static final String DEFAULT_TRACER_ID = "%{tname}/%{ip}";

    public static final String RECEIVER_LOAD_BALANCE_KEY = "receiver.load.balance";

    public static final String DEFAULT_RECEIVER_LOAD_BALANCE = "com.dlmu.bat.common.loadbalance.impl.ConsistentHashLoadBalance";

}
