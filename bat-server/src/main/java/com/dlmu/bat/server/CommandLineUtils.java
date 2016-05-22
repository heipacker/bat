package com.dlmu.bat.server;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import joptsimple.OptionParser;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class CommandLineUtils {

    /**
     * Print usage and exit
     */
    public static void printUsageAndDie(OptionParser optionParser, String message) throws IOException {
        System.err.println(message);
        optionParser.printHelpOn(System.err);
        System.exit(1);
    }

    /**
     * Parse key-value pairs in the form key=value
     */
    public static Properties parseKeyValueArgs(List<String> args, boolean acceptMissingValue) {
        Iterable<String[]> paramIterable = Iterables.filter(Iterables.transform(args, new Function<String, String[]>() {
            @Override
            public String[] apply(String input) {
                return input.split("=");
            }
        }), new Predicate<String[]>() {
            @Override
            public boolean apply(String[] input) {
                return input.length > 0;
            }
        });

        Properties props = new Properties();
        for (String[] paramPair : paramIterable) {
            if (paramPair.length == 1) {
                if (acceptMissingValue) {
                    props.put(paramPair[0], "");
                } else {
                    throw new IllegalArgumentException("Missing value for key ${a(0)}");
                }
            } else if (paramPair.length == 2) {
                props.put(paramPair[0], paramPair[1]);
            } else {
                System.err.println("Invalid command line properties: " + Joiner.on(" ").join(args));
                System.exit(1);
            }
        }
        return props;
    }
}
