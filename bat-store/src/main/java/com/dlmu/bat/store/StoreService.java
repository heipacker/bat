package com.dlmu.bat.store;

import com.dlmu.bat.common.BaseSpan;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public interface StoreService<T extends BaseSpan> {

    void storeSpan(T baseSpan);
}
