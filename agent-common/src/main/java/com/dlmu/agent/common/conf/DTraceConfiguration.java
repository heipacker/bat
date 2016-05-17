package com.dlmu.agent.common.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper which integrating applications should implement in order
 * to provide tracing configuration.
 */
public abstract class DTraceConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DTraceConfiguration.class);

  private static final Map<String, String> EMPTY_MAP = new HashMap<String, String>(1);

  /**
   * An empty HTrace configuration.
   */
  public static final DTraceConfiguration EMPTY = fromMap(EMPTY_MAP);

  /**
   * Create an HTrace configuration from a map.
   *
   * @param conf    The map to create the configuration from.
   * @return        The new configuration.
   */
  public static DTraceConfiguration fromMap(Map<String, String> conf) {
    return new MapConf(conf);
  }

  public static DTraceConfiguration fromKeyValuePairs(String... pairs) {
    if ((pairs.length % 2) != 0) {
      throw new RuntimeException("You must specify an equal number of keys " +
          "and values.");
    }
    Map<String, String> conf = new HashMap<String, String>();
    for (int i = 0; i < pairs.length; i+=2) {
      conf.put(pairs[i], pairs[i + 1]);
    }
    return new MapConf(conf);
  }

  public abstract String get(String key);

  public abstract String get(String key, String defaultValue);

  public boolean getBoolean(String key, boolean defaultValue) {
    String value = get(key, String.valueOf(defaultValue)).trim().toLowerCase();

    if ("true".equals(value)) {
      return true;
    } else if ("false".equals(value)) {
      return false;
    }

    LOG.warn("Expected boolean for key [" + key + "] instead got [" + value + "].");
    return defaultValue;
  }

  public int getInt(String key, int defaultVal) {
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

  private static class MapConf extends DTraceConfiguration {
    private final Map<String, String> conf;

    public MapConf(Map<String, String> conf) {
      this.conf = new HashMap<String, String>(conf);
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
