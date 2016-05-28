package com.dlmu.bat.store.impl;

import com.dlmu.bat.store.StoreService;
import com.google.common.base.Charsets;
import org.hbase.async.HBaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class HBaseStoreServiceImpl implements StoreService<HBaseBaseSpan> {

    private static final Logger logger = LoggerFactory.getLogger(HBaseStoreServiceImpl.class);

    private HBaseClient hBaseClient;

    protected byte[] table;

    public final byte[] FAMILY = "b".getBytes(Charsets.UTF_8);

    public HBaseStoreServiceImpl(HBaseClient hBaseClient, String table) {
        this.hBaseClient = hBaseClient;
        this.table = table.getBytes(Charsets.UTF_8);
    }

    @Override
    public void storeSpan(HBaseBaseSpan baseSpan) {
        //todo 添加code
    }
}
