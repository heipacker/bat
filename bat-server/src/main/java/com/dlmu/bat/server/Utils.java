package com.dlmu.bat.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by heipacker on 16-5-18.
 */
public class Utils {

    private static String appName;

    public static String getAppName() {
        return appName;
    }

    /**
     * Read a properties file from the given path
     * @param filename The path of the file to read
     */
    public static Properties loadProps(String filename) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        try (InputStream propStream = new FileInputStream(filename)) {
            props.load(propStream);
        }
        return props;
    }
}
