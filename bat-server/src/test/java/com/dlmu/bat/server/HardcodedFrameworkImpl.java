package com.dlmu.bat.server;

/**
 * Created by fupan on 16-4-9.
 */
public class HardcodedFrameworkImpl implements Framework {
    @Override
    public <T> T secure(Class<T> type) {
        if (type == Service.class) {
            return (T) new SecuredService();
        } else {
            throw new IllegalArgumentException("Unknown: " + type);
        }
    }
}