package com.dlmu.bat.common.register;

import com.dlmu.bat.common.conf.DTraceConfiguration;

import java.util.HashMap;

/**
 *
 * @author heipacker
 */
public class RegistryUtils {

    private static final String ZK_ADDRESS = "zk.address";

    public static String resolve() {
        DTraceConfiguration registries = DTraceConfiguration.fromMap(new HashMap<String, String>());
        return registries.get(ZK_ADDRESS);
    }
}
