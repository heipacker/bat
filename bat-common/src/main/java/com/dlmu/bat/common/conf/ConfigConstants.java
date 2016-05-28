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
     * The default batClient ID to use if no other ID is configured.
     */
    public static final String DEFAULT_TRACER_ID = "%{tname}/%{ip}";

    public static final String RECEIVER_LOAD_BALANCE_KEY = "receiver.load.balance";

    public static final String DEFAULT_RECEIVER_LOAD_BALANCE = "com.dlmu.bat.common.loadbalance.impl.ConsistentHashLoadBalance";

    public static final String SAMPLER_CLASS_KEY = "bat.samper.class";

    public static final String OVERRIDE_SAMPLE_ENABLED_KEY = "bat.override.sample.enabled";

    public static final boolean DEFAULT_OVERRIDE_SAMPLE_ENABLED = false;

    public static final String CLIENT_SEND_RETRIES_KEY = "bat.client.retries";

    public static final String RECEIVER_TYPE_KEY = "bat.receiver.type";

    public static final String ZK_ADDRESS_KEY = "bat.zk.address";

    public static final String DEFAULT_ZK_ADDRESS = "localhost:2181";

    public static final String SERVER_PORT_KEY = "bat.server.port";

    public static final int DEFAULT_SERVER_PORT = 7079;

    public static final String HBASE_ZK_ADDRESS_KEY = "bat.hbase.zk.address";

    public static final java.lang.String HBASE_ZK_PATH_KEY = "bat.hbase.zk.path";

    public static final java.lang.String HBASE_BAT_TRACE_TABLE_KEY = "bat";
}
