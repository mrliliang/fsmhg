package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledGraph;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);

        File output = new File(arguments.output);
        output.delete();
        File data = new File(arguments.data);
        TransLoader loader = new TransLoader(data);
        if (arguments.window <= 0) {
            FSMHG fsmhg = new FSMHG(arguments.support, arguments.maxEdgeNum,false, 0);
            fsmhg.setOutput(output);
            fsmhg.enumerate(loader.loadTrans());
            return;
        }

        output.mkdir();
        Enumerator enumerator;
        if (arguments.enumerator == Arguments.ENUM_FSMHG_WIN) {
            enumerator = new FSMHGWIN(arguments.support, arguments.maxEdgeNum, false, 0);
        } else {
            enumerator = new FSMHG(arguments.support, arguments.maxEdgeNum, false, 0);
        }
        List<LabeledGraph> trans = loader.loadTrans(arguments.window);
        int winCount = 0;
        while (trans.size() == arguments.window) {
            enumerator.setOutput(new File(output, String.format("WIN%03d", winCount++)));
            enumerator.enumerate(trans);
            trans = trans.subList(arguments.sliding, trans.size());
            trans.addAll(loader.loadTrans(arguments.sliding));
        }
    }

    private static class Arguments {
        public static final int ENUM_FSMHG = 1;
        public static final int ENUM_FSMHG_WIN = 2;
        public String data;
        public String output;
        public double support;
        public int maxEdgeNum = Integer.MAX_VALUE;
        public int window;
        public int sliding;
        public int enumerator = ENUM_FSMHG_WIN;


        private Arguments() {

        }

        public static Arguments parse(String[] args) {
            Options ops = new Options();
            ops.addOption("d", "data", true, "The path of data");
            ops.addOption("o", "output", true, "The file name of the result");
            ops.addOption("s", "support",true, "Support threshold");
            ops.addOption("m", "max-edge", true, "The maximal number of edges of a pattern");
            ops.addOption("w", "window size", true, "Open sliding window mode and specify the window size (>= 1)");
            ops.addOption("ss", "sliding speed", true, "Window sliding speed (>0 1)");
            ops.addOption("e", "enumerator", true, "1(FSMHG)/2(FSMHG-WIN)");
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

            if (!cmd.hasOption("w") && cmd.hasOption("ss")) {
                System.out.println("Must specify window size");
                System.exit(1);
            }

            if (cmd.hasOption("w")) {
                arguments.window = Integer.parseInt(cmd.getOptionValue("w"));
                if (arguments.window <= 0) {
                    System.out.println("Window size must be >= 1");
                    System.exit(1);
                }

                if (!cmd.hasOption("ss")) {
                    System.out.println("Must specify window sliding speed");
                    System.exit(1);
                }

                arguments.sliding = Integer.parseInt(cmd.getOptionValue("ss"));
                if (arguments.sliding <= 0) {
                    System.out.println("Window sliding speed must be >= 1");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("e")) {
                int e = Integer.parseInt(cmd.getOptionValue("e"));
                if (e != ENUM_FSMHG || e != ENUM_FSMHG_WIN) {
                    System.out.println("Invalid enumerator");
                    System.exit(1);
                }
                arguments.enumerator = e;
            }

            return arguments;
        }
    }
}