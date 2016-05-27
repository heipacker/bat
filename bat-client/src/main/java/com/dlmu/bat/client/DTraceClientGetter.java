package com.dlmu.bat.client;

import com.dlmu.bat.common.conf.DTraceConfiguration;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * @author heipacker
 * @date 16-5-21.
 */
public class DTraceClientGetter {

    private static final Supplier<DTraceClient> instance = Suppliers.memoize(new Supplier<DTraceClient>() {
        @Override
        public DTraceClient get() {
            return new DTraceClient(DTraceConfiguration.fromMap(System.getenv()));
        }
    });

    public static DTraceClient getClient() {
        return instance.get();
    }

}
