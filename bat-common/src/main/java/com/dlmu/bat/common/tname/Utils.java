package com.dlmu.bat.common.tname;

import com.dlmu.bat.plugin.TNameManager;
import com.google.common.base.Strings;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class Utils {

    public static String getTName() {
        ServiceLoader<TNameManager> tNameManagerServiceLoader = ServiceLoader.load(TNameManager.class);
        Iterator<TNameManager> iterator = tNameManagerServiceLoader.iterator();
        while (iterator.hasNext()) {
            TNameManager tNameManager = iterator.next();
            if (tNameManager == null) {
                continue;
            }
            String tname = tNameManager.tname();
            if (Strings.isNullOrEmpty(tname)) {
                continue;
            }
            return tname;
        }
        throw new RuntimeException("no find " + TNameManager.class.getName() + " implements.");
    }
}
