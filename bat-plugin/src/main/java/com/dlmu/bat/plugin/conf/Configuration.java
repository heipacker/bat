package com.dlmu.bat.plugin.conf;

import com.dlmu.bat.plugin.Plugin;

import java.util.Map;
import java.util.Properties;

/**
 * Wrapper which integrating applications should implement in order
 * to provide tracing configuration.
 */
public interface Configuration extends Iterable<Map.Entry<String, String>>, Plugin {

    void addConf(Configuration configuration);

    int index();

    void put(String key, String value);

    void putAll(Map<String, String> mapConf);

    void putAll(Properties properties);

    String get(String key);

    String get(String key, String defaultValue);

    Boolean getBoolean(String key);

    Boolean getBoolean(String key, boolean defaultValue);

    Integer getInt(String key);

    Integer getInt(String key, int defaultVal);

    interface ConfigurationListener extends ConfigListener<Configuration> {

    }

    /**
     * 注册监听器
     *
     * @param configListener
     * @param trigger
     */
    void addListener(ConfigurationListener configListener, boolean trigger);

    void addListener(ConfigurationListener configListener);

    /**
     * 实现类 实现如果支持热更新, 则需要调用这个回调函数出发内部更新
     */
    void callback();
}
