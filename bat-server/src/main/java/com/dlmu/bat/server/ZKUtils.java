package com.dlmu.bat.server;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class ZKUtils {

    public static String makePath(String parent, String child) {
        StringBuilder path = new StringBuilder();
        if (!parent.startsWith("/")) {
            path.append("/");
        }

        path.append(parent);
        if (child != null && child.length() != 0) {
            if (!parent.endsWith("/")) {
                path.append("/");
            }

            if (child.startsWith("/")) {
                path.append(child.substring(1));
            } else {
                path.append(child);
            }

            return path.toString();
        } else {
            return path.toString();
        }
    }

}
