package com.dlmu.bat.server.zk;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class ZKClientImpl implements ZKClient {
    @Override
    public void close() {

    }

    @Override
    public void deletePath(String node) {

    }

    @Override
    public void addConnectionChangeListenter(ConnectionStateListener connectionStateListener) {

    }

    @Override
    public boolean checkExist(String serverRoot) {
        return false;
    }

    @Override
    public void addPersistentNode(String serverRoot) {

    }

    @Override
    public void addEphemeralNode(String node) {

    }

    @Override
    public void incrementReference() {

    }

    public ZKClientImpl(String address) {
    }
}
