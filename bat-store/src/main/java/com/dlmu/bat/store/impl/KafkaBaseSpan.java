package com.dlmu.bat.store.impl;

import com.dlmu.bat.common.BaseSpan;

import java.io.IOException;

/**
 * @author heipacker
 * @date 16-6-8.
 */
public class KafkaBaseSpan extends BaseSpan {

    private byte[] sources;

    public KafkaBaseSpan(byte[] sources) throws IOException {
        super(sources);
    }
}
