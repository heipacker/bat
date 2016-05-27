package com.dlmu.bat.common.codec;

import java.io.OutputStream;

/**
 * 序列化器。
 *
 * @param <T> 序列化类型。
 * @author heipacker
 */
public interface Serializer<T> {

    void serialize(T source, OutputStream os) throws Exception;

}
