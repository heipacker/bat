package com.dlmu.bat.store;

import com.dlmu.bat.common.BaseSpan;
import com.stumbleupon.async.Deferred;

import java.util.List;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public interface StoreService<T extends BaseSpan> {

    Deferred<Object> storeSpan(T baseSpan);

    /**
     * @param traceId
     * @return
     */
    Deferred<List<T>> getChildrenSpans(String traceId);

    /**
     * @param traceId
     * @param parentSpanId
     * @return
     */
    Deferred<List<T>> getChildrenSpans(final String traceId, final String parentSpanId);
}
