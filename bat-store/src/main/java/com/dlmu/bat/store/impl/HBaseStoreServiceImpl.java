package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.TraceIdWrapper;
import com.dlmu.bat.common.metric.Metrics;
import com.dlmu.bat.store.StoreService;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;
import com.yammer.metrics.core.TimerContext;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
import org.hbase.async.KeyValue;
import org.hbase.async.PutRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class HBaseStoreServiceImpl implements StoreService<HBaseBaseSpan> {

    private static final Logger logger = LoggerFactory.getLogger(HBaseStoreServiceImpl.class);

    private HBaseClient hBaseClient;

    private byte[] table;

    private byte[] family = ascii2byte("b");

    /**
     * @param hBaseClient
     * @param table
     */
    public HBaseStoreServiceImpl(HBaseClient hBaseClient, String table) {
        this.hBaseClient = hBaseClient;
        this.table = table.getBytes(Charsets.UTF_8);
    }

    /**
     * @param hBaseClient
     * @param table
     * @param family
     */
    public HBaseStoreServiceImpl(HBaseClient hBaseClient, String table, String family) {
        this.hBaseClient = hBaseClient;
        this.table = table.getBytes(Charsets.UTF_8);
        this.family = ascii2byte(family);
    }

    @Override
    public void storeSpan(HBaseBaseSpan baseSpan) {
        byte[] rowKey = ascii2byte(baseSpan.rowKey());
        byte[] qualifier = ascii2byte(baseSpan.qualifier());
        PutRequest put = new PutRequest(table, rowKey, family, qualifier, baseSpan.toBytes());
        put.setDurable(false);//todo 可以修改 视情况而定

        final TimerContext context = Metrics.newTimer("storeSpanTimer", ImmutableMap.of("", "")).time();
        hBaseClient.put(put).addBoth(new Callback<Object, Object>() {

            @Override
            public Object call(Object arg) throws Exception {
                if (arg instanceof Throwable) {
                    Throwable e = (Throwable) arg;
                    logger.error("add HBaseBaseSpan error", e);
                    context.stop();
                }
                return arg;
            }
        });
    }

    /**
     * @param traceId
     * @param parentSpanId
     * @return
     */
    public Deferred<List<HBaseBaseSpan>> getChildrenSpans(final String traceId, final String parentSpanId) {
        TraceIdWrapper traceIdWrapper = TraceIdWrapper.parseTraceId(traceId);
        byte[] rowKey = ascii2byte(traceIdWrapper.rowKey());
        GetRequest getRequest = new GetRequest(table, rowKey, family);
        Deferred<List<HBaseBaseSpan>> deferred = hBaseClient.get(getRequest).addBoth(new Callback<List<HBaseBaseSpan>, ArrayList<KeyValue>>() {
            @Override
            public List<HBaseBaseSpan> call(ArrayList<KeyValue> arg) throws Exception {
                List<HBaseBaseSpan> result = new ArrayList<HBaseBaseSpan>();
                for (KeyValue keyValue : arg) {
                    byte[] value = keyValue.value();
                    HBaseBaseSpan hBaseBaseSpan = new HBaseBaseSpan(value);
                    result.add(hBaseBaseSpan);
                }
                if (parentSpanId != null && !parentSpanId.isEmpty()) {
                    Iterator<HBaseBaseSpan> iterator = result.iterator();
                    while (iterator.hasNext()) {
                        HBaseBaseSpan hBaseBaseSpan = iterator.next();
                        if (!isChild(parentSpanId, hBaseBaseSpan)) {
                            iterator.remove();
                        }
                    }
                }
                return result;
            }

            private boolean isChild(String parentSpanId, HBaseBaseSpan hBaseBaseSpan) {
                return hBaseBaseSpan.getSpanId() != null &&
                        hBaseBaseSpan.getSpanId().startsWith(parentSpanId) &&
                        !parentSpanId.equals(hBaseBaseSpan.getSpanId());
            }
        });
        return deferred;
    }

    /**
     * @param traceId
     * @return
     */
    public Deferred<List<HBaseBaseSpan>> getChildrenSpans(String traceId) {
        return getChildrenSpans(traceId, null);
    }


    static String byte2ascii(byte[] bytes) {
        char value[] = new char[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            value[i] = (char) bytes[i];
        }
        return String.valueOf(value);
    }

    static byte[] ascii2byte(String value) {
        int len = value.length();
        byte[] result = new byte[len];
        for (int i = 0; i < len; ++i) {
            result[i] = (byte) value.charAt(i);
        }
        return result;
    }

    public HBaseClient gethBaseClient() {
        return hBaseClient;
    }

    public void sethBaseClient(HBaseClient hBaseClient) {
        this.hBaseClient = hBaseClient;
    }

    public byte[] getTable() {
        return table;
    }

    public void setTable(byte[] table) {
        this.table = table;
    }

    public byte[] getFamily() {
        return family;
    }

    public void setFamily(byte[] family) {
        this.family = family;
    }
}
