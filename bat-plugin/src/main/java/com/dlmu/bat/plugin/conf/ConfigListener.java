package com.dlmu.bat.plugin.conf;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public interface ConfigListener<T> {

    void call(T t);
}
