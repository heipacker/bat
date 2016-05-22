package com.dlmu.bat.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class ServerConfig {

    public static ServerConfig fromProps(Properties serverProps) {
        return new ServerConfig(serverProps);
    }

    private Map<String, String> properties = new HashMap<String, String>();

    public ServerConfig() {

    }

    public ServerConfig(Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            this.properties.put((String) entry.getKey(), (String) entry.getValue());
        }
    }
}
