package com.dlmu.bat.server;

/**
 * Created by fupan on 16-4-9.
 */
public interface Framework {
    <T> T secure(Class<T> type);
}