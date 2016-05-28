package com.dlmu.bat.server;

import com.dlmu.bat.plugin.conf.Configuration;
import com.dlmu.bat.plugin.conf.impl.AbstractConfiguration;
import com.google.common.base.Joiner;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class Bat {

    private static final Logger logger = LoggerFactory.getLogger(Bat.class);

    private static Properties getPropsFromArgs(String[] args) throws IOException {
        OptionParser optionParser = new OptionParser();
        OptionSpec<String> overrideOpt = optionParser.accepts("override", "Optional property that should override values set in server.properties file")
                .withRequiredArg()
                .ofType(String.class);

        if (args.length == 0) {
            CommandLineUtils.printUsageAndDie(optionParser, "USAGE: java [options] %s server.properties [--override property=value]*".format(Bat.class.getSimpleName()));
        }

        Properties props = PropertiesUtils.loadProps(args[0]);
        if (args.length > 1) {
            OptionSet options = optionParser.parse(Arrays.copyOfRange(args, 1, args.length));
            if (options.nonOptionArguments().size() > 0) {
                CommandLineUtils.printUsageAndDie(optionParser, "Found non argument parameters: " + Joiner.on(",").join(options.nonOptionArguments()));
            }
            props.putAll(CommandLineUtils.parseKeyValueArgs(options.valuesOf(overrideOpt), true));
        }
        return props;
    }

    public static void main(String[] args) {
        try {
            final Properties serverProps = getPropsFromArgs(args);
            Configuration configuration = AbstractConfiguration.getConfiguration();
            configuration.putAll(serverProps);
            final BatServerStartable batServerStartable = BatServerStartable.fromConfig(configuration);

            // attach shutdown handler to catch control-c
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    batServerStartable.shutdown();
                }
            });

            batServerStartable.startup();
            batServerStartable.awaitShutdown();
        } catch (Throwable e) {
            logger.error("", e);
            System.exit(1);
        }
        System.exit(0);
    }
}
