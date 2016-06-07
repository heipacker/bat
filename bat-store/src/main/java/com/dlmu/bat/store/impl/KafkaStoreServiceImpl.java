package com.dlmu.bat.store.impl;

import com.dlmu.bat.store.StoreService;
import com.stumbleupon.async.Deferred;

import java.util.List;

/**
 * @author heipacker
 * @date 16-6-8.
 */
public class KafkaStoreServiceImpl implements StoreService<KafkaBaseSpan> {

    @Override
    public Deferred<Object> storeSpan(KafkaBaseSpan baseSpan) {
        //todo send to kafka
        return null;
    }

    /**
     * @param traceId
     * @return
     */
    @Override
    public Deferred<List<KafkaBaseSpan>> getChildrenSpans(String traceId) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param traceId
     * @param parentSpanId
     * @return
     */
    @Override
    public Deferred<List<KafkaBaseSpan>> getChildrenSpans(String traceId, String parentSpanId) {
        throw new UnsupportedOperationException();
    }
}
