package com.dlmu.bat.plugin.impl;

import com.dlmu.bat.plugin.TNameManager;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class DefaultTNameManager implements TNameManager {

    @Override
    public String tname() {
        return "testTName";
    }

    @Override
    public int index() {
        return 300;
    }
}
