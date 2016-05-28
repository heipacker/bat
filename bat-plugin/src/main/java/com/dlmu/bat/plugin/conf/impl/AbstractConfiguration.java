package com.dlmu.bat.plugin.conf.impl;

import com.dlmu.bat.plugin.ExtensionLoader;
import com.dlmu.bat.plugin.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Wrapper which integrating applications should implement in order
 * to provide tracing configuration.
 */
public abstract class AbstractConfiguration implements Configuration {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Create an HTrace configuration from a map.
     *
     * @param conf The map to create the configuration from.
     * @return The new configuration.
     */
    public static Configuration fromMap(Map<String, String> conf) {
        return new MapConf(conf);
    }

    public static Configuration fromKeyValuePairs(String... pairs) {
        if ((pairs.length % 2) != 0) {
            throw new RuntimeException("You must specify an equal number of keys " +
                    "and values.");
        }
        Map<String, String> conf = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            conf.put(pairs[i], pairs[i + 1]);
        }
        return new MapConf(conf);
    }

    /**
     * @return
     */
    public static Configuration getConfiguration() {
        return ExtensionLoader.getExtension(Configuration.class);
    }

    private final List<ConfigurationListener> listeners = new ArrayList<ConfigurationListener>();

    public void addConf(Configuration configuration) {
        for (Map.Entry<String, String> entry : configuration) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public abstract int index();

    public abstract void put(String key, String value);

    public void putAll(Map<String, String> mapConf) {
        for (Map.Entry<String, String> entry : mapConf.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void putAll(Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    public abstract String get(String key);

    public abstract String get(String key, String defaultValue);

    public Boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, String.valueOf(defaultValue)).trim().toLowerCase();

        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        }

        logger.warn("Expected boolean for key [" + key + "] instead got [" + value + "].");
        return defaultValue;
    }

    public Boolean getBoolean(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Expected boolean for key [" + key + "] instead got [" + value + "].");
    }

    /**
     * 注册监听器
     *
     * @param configListener
     * @param trigger
     */
    public void addListener(ConfigurationListener configListener, boolean trigger) {
        listeners.add(configListener);
        if (trigger) {
            configListener.call(this);
        }
    }

    public void addListener(ConfigurationListener configListener) {
        addListener(configListener, false);
    }

    /**
     * 实现类 实现如果支持热更新, 则需要调用这个回调函数出发内部更新
     */
    public void callback() {
        for (ConfigurationListener listener : listeners) {
            listener.call(this);
        }
    }

    public Integer getInt(String key, int defaultVal) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Bad value for '" + key + "': should be int");
        }
    }

    public Integer getInt(String key) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Bad value for '" + key + "': should be int");
        }
    }

    private static class MapConf extends AbstractConfiguration {
        private final Map<String, String> conf;

        public MapConf(Map<String, String> conf) {
            this.conf = new HashMap<String, String>(conf);
        }

        @Override
        public void put(String key, String value) {
            conf.put(key, value);
        }

        @Override
        public int index() {
            return 300;
        }

        /**
         * Returns an iterator over elements of type {@code T}.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return conf.entrySet().iterator();
        }

        @Override
        public String get(String key) {
            return conf.get(key);
        }

        @Override
        public String get(String key, String defaultValue) {
            String value = get(key);
            return value == null ? defaultValue : value;
        }
    }
}
