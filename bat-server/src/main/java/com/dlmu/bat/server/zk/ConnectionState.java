package com.dlmu.bat.server.zk;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public enum ConnectionState {
    CONNECTED, SUSPENDED, RECONNECTED, LOST, READ_ONLY;
}