package com.dlmu.bat.common.metric;

import com.google.common.base.Strings;
import com.yammer.metrics.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * @author heipacker
 * @date 16-5-21.
 */
public class Metrics {

    private static String toScope(Map<String, String> tags) {
        Map<String, String> result = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (entry.getValue() != null || "".equals(entry.getValue())) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        if (!result.isEmpty()) {
            // convert dot to _ since reporters like Graphite typically use dot to represent hierarchy
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : result.entrySet()) {
                if (!first) {
                    sb.append(".");
                } else {
                    first = false;
                }
                sb.append(entry.getKey() + "=" + entry.getValue());
            }

            return sb.toString();
        }
        return null;
    }

    private static String toMBeanName(Map<String, String> tags) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (entry.getValue() != null || "".equals(entry.getValue())) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        if (!result.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : result.entrySet()) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append(entry.getKey() + "=" + entry.getValue());
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * Creates a new MetricName object for gauges, meters, etc. created for this
     * metrics group.
     *
     * @param name Descriptive name of the metric.
     * @param tags Additional attributes which mBean will have.
     * @return Sanitized metric name object.
     */
    private static MetricName metricName(String name, Map<String, String> tags) {
        Class klass = Metrics.class;
        String pkg = (klass.getPackage() == null) ? "" : klass.getPackage().getName();
        String simpleName = klass.getSimpleName().replaceAll("\\$$", "");
        return explicitMetricName(pkg, simpleName, name, tags);
    }

    private static MetricName explicitMetricName(String group, String typeName, String name, Map<String, String> tags) {
        StringBuilder nameBuilder = new StringBuilder();

        nameBuilder.append(group);

        nameBuilder.append(":type=");

        nameBuilder.append(typeName);

        if (name.length() > 0) {
            nameBuilder.append(",name=");
            nameBuilder.append(name);
        }

        String scope = toScope(tags);
        String tagsName = toMBeanName(tags);
        if (Strings.isNullOrEmpty(tagsName)) {
            nameBuilder.append(",").append(tagsName);
        }

        return new MetricName(group, typeName, name, scope, nameBuilder.toString());
    }

    /**
     *
     * @param name
     * @param metric
     * @param tags
     * @param <T>
     * @return
     */
    public static <T> Gauge<T> newGauge(String name, Gauge<T> metric, Map<String, String> tags) {
        return com.yammer.metrics.Metrics.defaultRegistry().newGauge(metricName(name, tags), metric);
    }

    /**
     *
     * @param name
     * @param eventType
     * @param timeUnit
     * @param tags
     * @param <T>
     * @return
     */
    public static <T> Meter newMeter(String name, String eventType, TimeUnit timeUnit, Map<String, String> tags) {
        return com.yammer.metrics.Metrics.defaultRegistry().newMeter(metricName(name, tags), eventType, timeUnit);
    }

    /**
     *
     * @param name
     * @param biased
     * @param tags
     * @param <T>
     * @return
     */
    public static <T> Histogram newHistogram(String name, Boolean biased, Map<String, String> tags) {
        return com.yammer.metrics.Metrics.defaultRegistry().newHistogram(metricName(name, tags), biased);
    }

    /**
     *
     * @param name
     * @param durationUnit
     * @param rateUnit
     * @param tags
     * @param <T>
     * @return
     */
    public static <T> Timer newTimer(String name, TimeUnit durationUnit, TimeUnit rateUnit, Map<String, String> tags) {
        return com.yammer.metrics.Metrics.defaultRegistry().newTimer(metricName(name, tags), durationUnit, rateUnit);
    }

    /**
     *
     * @param name
     * @param tags
     */
    public static void removeMetric(String name, Map<String, String> tags) {
        com.yammer.metrics.Metrics.defaultRegistry().removeMetric(metricName(name, tags));
    }

}