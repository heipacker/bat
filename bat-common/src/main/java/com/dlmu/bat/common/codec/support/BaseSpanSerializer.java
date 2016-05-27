package com.dlmu.bat.common.codec.support;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.codec.Serializer;

import java.io.OutputStream;

/**
 * BaseSpan序列化。
 *
 * @author heipacker
 */
public class BaseSpanSerializer implements Serializer<BaseSpan> {

    @Override
    public void serialize(BaseSpan source, OutputStream os) throws Exception {
        source.write(os);
    }
}
