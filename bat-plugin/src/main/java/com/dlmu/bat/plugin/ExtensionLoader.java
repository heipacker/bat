package com.dlmu.bat.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class ExtensionLoader {

    /**
     * @return
     */
    public static <T> T getExtension(Class<?> klass) {
        if (!Plugin.class.isAssignableFrom(klass)) {
            ServiceLoader<?> serviceLoader = ServiceLoader.load(klass);
            return (T) serviceLoader.iterator().next();
        }
        ServiceLoader<? extends Plugin> traceConfigurations = ServiceLoader.load((Class<? extends Plugin>) klass);
        int minIndex = Integer.MAX_VALUE;
        T conf = null;
        for (Plugin plugin : traceConfigurations) {
            if (plugin.index() < minIndex) {
                minIndex = plugin.index();
                conf = (T) plugin;
            }
        }
        return conf;
    }

    public static <T> List<T> getExtensionList(Class<?> klass) {
        List<T> result = new ArrayList<T>();
        ServiceLoader<?> serviceLoader = ServiceLoader.load(klass);
        Iterator<?> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            result.add((T) iterator.next());
        }
        return result;
    }
}
