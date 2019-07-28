package graft;

import org.apache.commons.configuration2.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graft.db.GraphUtil;
import graft.phases.DotPhase;
import graft.phases.BuildCpgPhase;
import graft.utils.LogUtil;

/**
 * TODO: javadoc
 */
public class Graft {

    private static Logger log = LoggerFactory.getLogger(Graft.class);

    /**
     * TODO: javadoc
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        validateArgs(args);
        String srcRoot = args[0];

        Configuration config = null;
        try {
            if (args.length == 2) {
                config = ConfigHelper.getFromFile(args[1]);
            } else {
                log.info("No config file specified, using default config");
                config = ConfigHelper.getDefaultConfig();
            }
        } catch (GraftException e) {
            log.error("Unable to configure Graft", e);
            System.exit(1);
        }
        LogUtil.setLogLevel(config.getString("general.log-level"));
        log.debug("Running with configuration {}", config.toString());

        GraphUtil.initGraph();

        GraftRun graftRun = new GraftRun(config);
        graftRun.register(
                new BuildCpgPhase(BuildCpgPhase.getOptions(config)),
                new DotPhase(DotPhase.getOptions(config))
        );

        log.info("Running Graft on source root {}", srcRoot);
        GraftResult result = graftRun.run();
        output(result.toString());
    }

    private static void validateArgs(String[] args) {
        if (args.length < 1) {
            log.error("Invalid command line arguments: no source root specified");
            System.exit(1);
        }
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(arg).append(" ");
            }
            log.debug("Running with command line arguments: {}", sb.toString());
        }
    }

    private static void output(String s) {
        System.out.println(s);
    }

}
