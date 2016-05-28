package com.dlmu.bat.common.codec;

/**
 * 解序列化器。
 *
 * @param <T> 序列化类型。
 * @author heipacker
 */
public interface Deserializer<T> {

    T deserialize(byte[] sources) throws Exception;
}
