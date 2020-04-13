package com.liang.fsmhg;

import org.apache.commons.cli.*;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);

        File data = new File(arguments.data);
        File output = new File(arguments.output);
        FSMHG fsmhg = new FSMHG(data, output, arguments.support, arguments.maxEdgeNum,false, 0);
        fsmhg.enumerate();
    }

    private static class Arguments {
        public String data;
        public String output;
        public double support;
        public int maxEdgeNum = Integer.MAX_VALUE;

        private Arguments() {

        }

        public static Arguments parse(String[] args) {
            Options ops = new Options();
            ops.addOption("d", "data", true, "The path of data");
            ops.addOption("o", "output", true, "The file name of the result");
            ops.addOption("s", "support",true, "Support threshold");
            ops.addOption("m", "max-edge", true, "The maximal number of edges of a pattern");
            ops.addOption("h", "Help");

            HelpFormatter formatter = new HelpFormatter();
            if (args == null || args.length == 0) {
                formatter.printHelp("java -jar fsmhg.jar", ops);
                System.exit(1);
            }

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(ops, args);
                if (cmd.hasOption("h")) {
                    formatter.printHelp("java -jar fsmhg.jar", ops);
                    System.exit(1);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Arguments arguments = new Arguments();
            arguments.data = cmd.getOptionValue("d");
            arguments.output = cmd.getOptionValue("o");
            if (!cmd.hasOption("o")) {
                arguments.output = arguments.data + "_result";
            }
            if (!cmd.hasOption("s")) {
                System.out.println("support value is required.");
                System.exit(1);
            }
            arguments.support = Double.parseDouble(cmd.getOptionValue("s"));
            if (cmd.hasOption("m")) {
                arguments.maxEdgeNum = Integer.parseInt(cmd.getOptionValue("m"));
            }
            return arguments;
        }
    }
}