package com.dlmu.bat.server.zk;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public interface ZKClient {

    void incrementReference();

    void close();

    void deletePath(String node);

    void addConnectionChangeListenter(ConnectionStateListener connectionStateListener);

    boolean checkExist(String serverRoot);

    void addPersistentNode(String serverRoot);

    void addEphemeralNode(String node);
}
