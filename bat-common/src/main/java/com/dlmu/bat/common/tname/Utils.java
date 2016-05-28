package com.dlmu.bat.common.tname;

import com.dlmu.bat.plugin.ExtensionLoader;
import com.dlmu.bat.plugin.TNameManager;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class Utils {

    private static final TNameManager tnameManager = ExtensionLoader.getExtension(TNameManager.class);

    public static String getTName() {
        return tnameManager.tname();
    }
}
