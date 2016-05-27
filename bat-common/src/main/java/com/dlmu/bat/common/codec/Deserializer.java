package com.dlmu.bat.common.codec;

/**
 * 解序列化器。
 * 
 * @author heipacker
 *
 * @param <T> 序列化类型。
 */
public interface Deserializer<T> {

	T deserialize(byte[] sources) throws Exception;
}
