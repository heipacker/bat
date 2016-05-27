package com.dlmu.bat.server.zk;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public interface ConnectionStateListener {
    void stateChanged(ZKClient sender, ConnectionState state);
}