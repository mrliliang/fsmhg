package com.liang.fsmhg;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        System.out.println("hello");
    }

    private static class Arguments {

        private Arguments() {

        }

        public static Arguments parse(String[] args) {
            Options ops = new Options();
            ops.addOption("d", "data", true, "The path of data");
            ops.addOption("o", "output", true, "The file name of the result");
            ops.addOption("s", "support",true, "Support threshold");
            ops.addOption("m", "max-edge", true, "The maximal number of edges of a pattern");
            ops.addOption("h", "Help");

            Arguments arguments = new Arguments();
            HelpFormatter formatter = new HelpFormatter();
            if (args == null || args.length == 0) {
                formatter.printHelp("java -jar fsmhg.jar", ops);
                System.exit(1);
            }
            return arguments;
        }
    }
}