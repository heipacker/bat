package com.dlmu.bat.client;

import com.dlmu.bat.plugin.conf.impl.AbstractConfiguration;
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
            return new DefaultDTraceClient(AbstractConfiguration.getConfiguration());
        }
    });

    public static DTraceClient getClient() {
        return instance.get();
    }

}
