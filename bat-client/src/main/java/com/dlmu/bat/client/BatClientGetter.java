package com.dlmu.bat.client;

import com.dlmu.bat.plugin.conf.impl.AbstractConfiguration;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * batClient getter, like factory pattern
 *
 * @author heipacker
 * @date 16-5-21.
 */
public class BatClientGetter {

    /**
     * lazy initialize
     */
    private static final Supplier<BatClient> instance = Suppliers.memoize(new Supplier<BatClient>() {
        @Override
        public BatClient get() {
            return new DefaultBatClient(AbstractConfiguration.getConfiguration());
        }
    });

    /**
     * @return
     */
    public static BatClient getClient() {
        return instance.get();
    }

}
